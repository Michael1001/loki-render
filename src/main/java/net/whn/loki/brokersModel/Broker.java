package net.whn.loki.brokersModel;

import net.whn.loki.common.*;
import net.whn.loki.error.ErrorHelper;
import net.whn.loki.error.MasterFrozenException;
import net.whn.loki.io.MasterIOHelper;
import net.whn.loki.master.MasterR;
import net.whn.loki.messaging.*;
import net.whn.loki.network.BrokerStreamSocket;
import net.whn.loki.network.Header;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * It sits on socket, waiting to receive
 */
public class Broker implements Runnable, ICommon {

    private static final String className = Broker.class.toString();
    private static final Logger log = Logger.getLogger(className);
    private static long gruntIDCounter = 0;
    private long gruntID;
    private final MasterR master;
    private final BrokersModel brokersModel;
    private String lastTaskTime;
    private Machine machine;
    private volatile MachineUpdate lastMachineUpdate;
    private GruntStatus gruntStatus;
    private volatile String gruntStatusString;
    private volatile String gruntErrorDetails;
    private Thread brokerThread;
    private BrokerStreamSocket brokerStreamSocket;
    private volatile Task assignedTask; //accessed by both master and broker
    private volatile boolean taskPending; //both master and broker

    private BlockingQueue<Header> headerBlockingQueue = new ArrayBlockingQueue<>(1);
    private ReentrantLock lock = new ReentrantLock();

    Broker(Socket gSocket, Socket fileReceiveSocket, Socket updateMachineSocket, MasterR masterR, BrokersModel bModel) throws IOException {

        gruntID = gruntIDCounter++;
        brokerStreamSocket = new BrokerStreamSocket(gSocket, fileReceiveSocket, updateMachineSocket);   //throws SocketException
        machine = new Machine("fetching...");
        master = masterR;
        brokersModel = bModel;
        gruntStatus = GruntStatus.BUSY;
        taskPending = false;
        gruntStatusString = "unknown";
        assignedTask = null;
        lastMachineUpdate = null;
        lastTaskTime = null;

        //log.setLevel(Level.ALL);
        new Thread(new SocketListener()).start();
        new Thread(new MachineSocketListener()).start();
    }

    @Override
    public void run() {
        while (true) {
            //we'll block on this call until we receive a header
            try {
                Header header = headerBlockingQueue.take();
                HeaderType headerType = header.getHeaderType();

                switch (headerType) {

                    case MACHINE_INFO:
                    case MACHINE_UPDATE:
                    case TELL_LOKI_VERSION:
                        handleIncomingDelivery(header);
                        break;
                    default:
                        lock.lock();
                        try {
                            handleIncomingDelivery(header);
                        } finally {
                            lock.unlock();
                        }
                }
            } catch (MasterFrozenException | InterruptedException e) {
                ErrorHelper.outputToLogMsgAndKill(null, false, log, "fatal error. exiting.", e);

            } catch (IOException e) {
                if (!brokerStreamSocket.isClosed()) {
                    log.throwing(className, "run()", e);
                    brokerStreamSocket.tryClose();  //and try to close the socket
                }
                break;  //now exit so this broker can die
            }
        }
    }
    
    public void sendBusyGruntLostMsg() throws InterruptedException, MasterFrozenException {
        log.fine("sending lostBusyGruntMessage for grunt w/ id: " + gruntID);
        Message lostBusyGruntMessage = new LostBusyGruntMessage(gruntID, assignedTask);
        master.deliverMessage(lostBusyGruntMessage);
    }

    public void sendRemoveGruntMessage() throws InterruptedException, MasterFrozenException {
        log.fine("sending removeGruntMessage for grunt w/ id: " + gruntID);
        Message removeGruntMessage = new RemoveGruntMessage(gruntID, gruntStatus, assignedTask);
        master.deliverMessage(removeGruntMessage);
    }

    public Object getValue(int column) {
        switch (column) {
            case 0:
                return machine.getHostname();
            case 1:
                return machine.getOsName();
            case 2:
                return machine.getProcessors();
            case 3:
                return lastMachineUpdate != null ? lastMachineUpdate.getSystemCpuLoad() : null;
            case 4:
                return lastMachineUpdate != null ? lastMachineUpdate.getMemUsageStr() : null;
            case 5:
                return lastTaskTime;
            case 6:
                switch (gruntStatus) {

                    case IDLE:
                        gruntStatusString = "idle";
                        break;
                    case RECEIVING:
                        if (lastMachineUpdate != null) {
                            Integer percent = lastMachineUpdate.getSentPercent();
                            if (percent != null) {
                                gruntStatusString = "receiving..." + percent + "%";
                            }
                        } else {
                            gruntStatusString = "receiving";
                        }
                        break;
                    case BUSY:
                        gruntStatusString = "busy";
                        break;
                    case SENDING:
                        Integer percent = lastMachineUpdate.getSentPercent();
                        if (percent != null) {
                            gruntStatusString = "sending..." + percent + "%";
                        } else {
                            gruntStatusString = "sending";
                        }
                        break;
                    case ERROR:
                        gruntStatusString = "error";
                }
                return gruntStatusString;
            default:
                throw new IllegalArgumentException(Integer.toString(column));
        }
    }

    public GruntDetails getDetails() {
        return new GruntDetails(
                machine.getHostname(),
                machine.getOsName(),
                machine.getOsVersion(),
                machine.getOsArchitecture(),
                machine.getProcessors(),
                machine.getTotalMemory(),
                machine.getTotalSwap(),
                machine.getUserName(),
                machine.getUserHome(),
                machine.getCurrentWorkingDir(),
                gruntStatusString,
                gruntErrorDetails
        );
    }

    int getCoreCount() {
        return machine.getProcessors();
    }

    /**
     * called by gruntsModel(aka master)
     * @return
     */
    long getGruntID() {
        return gruntID;
    }

    /**
     * only called by master
     * @return
     */
    GruntStatus getGruntStatus() {
        return gruntStatus;
    }

    /**
     * only called by master
     * @param gruntStatus
     */
    void setGruntStatus(GruntStatus gruntStatus) {
        this.gruntStatus = gruntStatus;
        this.gruntStatusString = gruntStatus.toString().toLowerCase();
    }

    void setGruntStatus(GruntStatus gruntStatus, String gruntLokiVersion) {
        setGruntStatus(gruntStatus);
        gruntErrorDetails = String.format(Main.differentLokiVersionsErrorTemplate, gruntLokiVersion, Main.LOKI_VERSION);
    }

    /**
     * cases like 'fetching...' or 'sending'; these are cosmetic and don't
     * affect internal behavior
     */
    void setGruntStatusString(String gruntStatusString) {
        this.gruntStatusString = gruntStatusString;
    }

    void setThread(Thread bThread) {
        brokerThread = bThread;
    }

    Thread getThread() {
        return brokerThread;
    }

    /**
     * called by master to send a task to this broker's grunt
     * @param task
     * @throws IOException if we get socket IO problem - pass to calling method
     */
    synchronized void sendTaskAssign(Task task) throws IOException {
        try {
            //clone so i send unique task object for grunt
            brokerStreamSocket.sendHeader(new Header(HeaderType.TASK_ASSIGN, task.clone()));
            assignedTask = task;
            taskPending = true;
        } catch (CloneNotSupportedException cex) {
            log.severe("couldn't clone task!");
        }

    }

    synchronized void sendTaskAbort() throws IOException {
        brokerStreamSocket.sendHeader(new Header(HeaderType.TASK_ABORT));
    }

    synchronized void sendQuit() throws IOException {
        brokerStreamSocket.sendHeader(new Header(HeaderType.QUIT_AFTER_TASK));
    }

    /**
     * we send a message to the grunt that we're shutting down, then we
     * close the socket so the broker thread knows we're shutting down
     */
    synchronized void shutdown() throws IOException {
        brokerStreamSocket.sendHeader(new Header(HeaderType.MASTER_SHUTDOWN));
        brokerStreamSocket.tryClose();
    }

    boolean isSocketClosed() {
        return brokerStreamSocket.isClosed();
    }

    /**
     * called by file sender, not master
     * NOTE: synchronize on the broker object because the master may try to send
     * a shutdown message or something similar on the socket while we're sending
     * ; master needs to synchronize on the broker as well!
     * @param projectFileObject
     * @throws IOException
     */
    synchronized void sendFile(ProjectFileObject projectFileObject) throws IOException {

        brokerStreamSocket.sendHeader(new Header(HeaderType.FILE_REPLY, projectFileObject.getName(), projectFileObject.getSize()));
        MasterIOHelper.sendProjectFileToGrunt(projectFileObject, brokerStreamSocket);
    }

    /**
     * This is handling an incoming delivery on the gruntSocket
     * It's meaning that first Grunt will send a message, and this Broker, what is mediator between Grunt and Master
     * , will call this method, to tell this message to the master.
     *
     * @param header
     * @throws InterruptedException if we timeout while delivering message to master
     * @throws MasterFrozenException
     */
    private void handleIncomingDelivery(Header header) throws InterruptedException, MasterFrozenException, IOException {

        HeaderType type = header.getHeaderType();
        switch (type) {

            case TELL_LOKI_VERSION:
                brokerStreamSocket.sendMasterLokiVersion(Main.LOKI_VERSION);
                break;
            case IDLE:
                handleIdle();
                break;
            case MACHINE_INFO:
                handleMachineInfo(header);
                break;
            case MACHINE_UPDATE:
                handleMachineUpdate(header);
                break;
            case FILE_REQUEST:
                handleFileRequest(header);
                break;
            case BUSY:
                handleBusyGrunt();
                break;
            case TASK_REPORT:
                handleTaskReport(header);   //throws InterruptedException
                taskPending = false;
                break;
            case DIFFERENT_LOKI_VERSION:
                setGruntStatus(GruntStatus.ERROR, header.getLokiVersion());
                brokerStreamSocket.confirmInformationReceived();
                break;
            default:
                log.severe("handleIncomingDelivery received and unknown header " + type.toString());
        }
    }

    private void handleBusyGrunt() {
        setGruntStatus(GruntStatus.BUSY);
        brokersModel.updateGruntRow(gruntID);
    }

    /**
     * determines if this is our first machineInfo for this grunt then
     * passes this info on to master along with the machineinfo object
     * @param header
     */
    private void handleMachineInfo(Header header) throws InterruptedException, MasterFrozenException {
        boolean firstReceive = machine.getProcessors() == -1;
        machine = header.getMachine();
        master.deliverMessage(new UpdateGruntMessage(gruntID, firstReceive));
    }

    public void handleMachineUpdate(Header header) throws InterruptedException, MasterFrozenException {
        lastMachineUpdate = header.getMachineUpdate();
        master.deliverMessage(new UpdateGruntMessage(gruntID, false));
    }

    /**
     * looks at report, if done, receives accompanying file as well,
     * then passes taskreport to master
     * @param header
     * @throws InterruptedException if we timeout on the master's delivery
     */
    private void handleTaskReport(Header header) throws InterruptedException, MasterFrozenException {
        //cast
        TaskReportMessage taskReportMessage = new TaskReportMessage(header.getTaskReport());

        //insert gruntID into report
        taskReportMessage.getReport().setGruntID(gruntID);
        Task task = taskReportMessage.getReport().getTask();

        //we only get a file if task is done
        if (task.getStatus() == TaskStatus.DONE) {
            lastTaskTime = task.getTaskTime();
            if (task.isAutoFileTranfer()) {
                updateTableRowSendingGrunt();
                if (!MasterIOHelper.receiveOutputFilesFromGrunt(task, this)) {
                    //failed to receive or save output file
                    task.setStatus(TaskStatus.FAILED);
                    String error = "failed to receive file from grunt, or " +
                            "save file to the output directory. Check " +
                            "output directory permissions.";
                    log.warning(error);
                    task.setErrout(error);
                }
            }
        }
        task.setGruntName(machine.getHostname());

        master.deliverMessage(taskReportMessage);
    }

    private void updateTableRowSendingGrunt() {
        setGruntStatus(GruntStatus.SENDING);
        brokersModel.updateGruntRow(gruntID);
    }

    private void handleIdle() throws InterruptedException, MasterFrozenException {
        master.deliverMessage(new IdleGruntMessage(gruntID));
    }

    /**
     * Inserts gruntID then passes to master to send the file to grunt.
     * @param header
     * @throws InterruptedException
     */
    private void handleFileRequest(Header header) throws InterruptedException, MasterFrozenException {
        master.deliverMessage(new FileRequestMessage(header.getProjectFileName(), gruntID));
    }

    public MasterR getMaster() {
        return master;
    }

    public BrokerStreamSocket getBrokerStreamSocket() {
        return brokerStreamSocket;
    }

    public BlockingQueue<Header> getHeaderBlockingQueue() {
        return headerBlockingQueue;
    }

    private class SocketListener implements Runnable {

        @Override
        public void run() {
            while (true) {
                Header header = null;
                try {
                    lock.lock();
                    try {
                        //we'll block on this call until we receive a header from socket
                        header = brokerStreamSocket.receiveDelivery();
                    } finally {
                        lock.unlock();
                        if (header != null) {
                            headerBlockingQueue.offer(header);
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                        }
                    }
                } catch (IOException ex) {
                    if (!brokerStreamSocket.isClosed()) {
                        //we're the first to hit the problem, so log it
                        log.throwing(className, "run()", ex);
                        brokerStreamSocket.tryClose();  //and try to close the socket
                    }
                    break;  //now exit so this broker can die
                } catch (ClassNotFoundException ex) {
                    try {
                        master.deliverMessage(new FatalThrowableMessage(MessageType.FATAL_ERROR, ex));
                    } catch (InterruptedException iex) {
                        //nothing to do here
                    } catch (MasterFrozenException mfe) {
                        ErrorHelper.outputToLogMsgAndKill(null, false, log, "fatal error. exiting.", ex);
                    }
                    log.throwing(className, "run()", ex);
                    break;
                }
            }
            brokerStreamSocket.tryClose();
            try {
                if (taskPending) {
                    sendBusyGruntLostMsg();
                }
                sendRemoveGruntMessage();

            } catch (InterruptedException | MasterFrozenException e) {
                ErrorHelper.outputToLogMsgAndKill(null, false, log, "fatal error. exiting.", e);
                System.exit(-1);
            }
        }
    }

    private class MachineSocketListener implements Runnable {

        @Override
        public void run() {
            while (true) {
                //we'll block on this call until we receive a header from socket
                try {
                    headerBlockingQueue.offer(brokerStreamSocket.receiveMachineDelivery());
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
