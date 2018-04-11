package net.whn.loki.grunt;

import net.whn.loki.commandLine.CommandLineHelper;
import net.whn.loki.commandLine.ProcessHelper;
import net.whn.loki.common.*;
import net.whn.loki.common.configs.Config;
import net.whn.loki.error.ErrorHelper;
import net.whn.loki.io.GruntIOHelper;
import net.whn.loki.io.IOHelper;
import net.whn.loki.master.MasterEQCaller;
import net.whn.loki.master.MasterR;
import net.whn.loki.network.GruntStreamSocket;
import net.whn.loki.network.Header;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class GruntR implements Runnable, ICommon {

    private static final String className = "net.whn.loki.grunt.GruntR";
    private static final Logger log = Logger.getLogger(className);

    public static File MAIN_FOLDER;
    public static File CACHE_FOLDER;
    public static File RENDER_FOLDER;
    public static final String MAIN_FOLDER_NAME = "grunt";
    public static final String CACHE_FOLDER_NAME = "fileCache";
    public static final String RENDER_FOLDER_NAME = "renderFolder";

    private static MasterR masterR;
    private static Config config;
    private static String masterLokiVersion;
    private String masterName;
    private static GruntForm gruntForm;
    private static boolean gruntcl = false;
    private static GruntStatus status;
    private static volatile boolean localShutdown;
    private final ExecutorService taskHandler;
    private FutureTask<String> runningTask;
    private ScheduledExecutorService machineUpdateHandler;
    private volatile Task task; //receiver puts, taskHandler null when done
    private static boolean gruntQuitting;
    //multicast
    private MulticastSocket mSock;
    //socket - stream
    private GruntStreamSocket gruntStreamSock;

    public GruntR(Config config) {

        GruntR.config = config;

        localShutdown = false;
        gruntQuitting = false;

        taskHandler = Executors.newSingleThreadExecutor();
    }

    public GruntR(MasterR masterR, Config config) {

        GruntR.masterR = masterR;

        if (GruntR.masterR != null) {
            GruntR.masterR.setGrunt(this); //we're w/ the manager on this computer
        }

        GruntR.config = config;

        localShutdown = false;
        gruntQuitting = false;

        taskHandler = Executors.newSingleThreadExecutor();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        do {    //while !localShutdown
            if (canGetMasterAddress()) {
                try {   //initial setup steps in this try

                    boolean connected = false;
                    InetAddress masterIp = config.getMasterIp();

                    while (!connected && !localShutdown) {
                        try {
                            //throws IOE if get any problems w/ socket/stream setup
                            gruntStreamSock = new GruntStreamSocket(masterIp, config.getConnectPort(), config.getFilesReceivePort(), config.getUpdateMachinePort());
                            connected = true;

                        } catch (IOException ioex) {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException ex) {
                            }
                        }
                    }

                    if (connected) {
                        gruntStreamSock.sendMachineHeader(new Header(null,HeaderType.TELL_LOKI_VERSION));
                        GruntR.masterLokiVersion = gruntStreamSock.getMasterLokiVersion();

                        String gruntLokiVersion = config.getLOKI_VERSION();
                        if (!GruntR.masterLokiVersion.equals(gruntLokiVersion)) {

                            Machine machine = new Machine();
                            gruntStreamSock.sendMachineHeader(new Header(HeaderType.MACHINE_INFO, machine));
                            gruntStreamSock.sendMachineHeader(new Header(HeaderType.MACHINE_UPDATE, machine.getMachineUpdate()));

                            gruntStreamSock.sendHeader(new Header(gruntLokiVersion, HeaderType.DIFFERENT_LOKI_VERSION));
                            gruntStreamSock.waitUntilInformationReceived();
                            localShutdown = true;
                        }
                    }

                    if (!localShutdown) {
                        machineUpdateHandler = Executors.newSingleThreadScheduledExecutor();

                        masterName = masterIp.getHostName();
                        String msg = "online with master '" + masterName + "'";
                        if (gruntForm != null) {
                            GruntEQCaller.invokeUpdateConnectionLbl(gruntForm, msg);
                        } else {
                            System.out.println(msg);
                        }

                        machineUpdateHandler.scheduleWithFixedDelay(new MachineUpdateR(gruntStreamSock), 0, 500, TimeUnit.MILLISECONDS);

                        if (task == null) { //tell master we're idle
                            gruntStreamSock.sendHeader(new Header(HeaderType.IDLE));
                        } else {  //didn't conclude last task so do it now
                            AssignedTask myTask = new AssignedTask();
                            runningTask = new FutureTask<String>(myTask);
                            taskHandler.submit(runningTask);
                        }

                        //main receive loop
                        do {
                            //we block on the receiveDelivery; break out when we lost connection
                        } while (!couldHandleDelivery(gruntStreamSock.receiveDelivery()));

                    }
                    if (machineUpdateHandler != null) {
                        machineUpdateHandler.shutdownNow();
                    }
                    if (gruntStreamSock != null) {
                        gruntStreamSock.tryClose();
                    }

                    //these four are all runtime problems - fatal
                } catch (ClassNotFoundException | InvalidClassException | StreamCorruptedException | OptionalDataException ex) {
                    handleFatalException(ex);
                } catch (IOException ex) {
                    /**
                     * come here if:
                     * 1. socket was closed by user initiated shutdown
                     * 2. we lost socket because of other IOE or task handler
                     * closed socket
                     */
                    if (!localShutdown) { //case 1
                        //user closed the socket; shutdown
                        //not the user, so we lost socket from IOE; cleanup
                        //and go back to the beginning: findmaster, etc..
                        log.throwing(className, "run()", ex);
                        gruntStreamSock.tryClose();
                    }
                }
                String msg = "attempting to connect with master...";
                if (gruntForm != null) {
                    GruntEQCaller.invokeUpdateConnectionLbl(gruntForm, msg);
                } else {
                    System.out.print(msg);
                }


            } else {  //findmaster() failed: IOE or user signalled quit
                localShutdown = true;   //either case is shutdown
            }
        } while (!localShutdown);
        shutdown(false);
    }

    public void setGruntForm(GruntForm gForm) {
        gruntForm = gForm;
        gruntcl = true;
    }

    public Config getCfg() {
        return config;
    }

    /**
     * could be called by AWT EQ (user abort/shutdown), master, or receiverThread
     * (master abort)
     */
    public void abortCurrentTask(TaskStatus abortType) {
        if (task != null) {
            if (task.getStatus() == TaskStatus.READY) {
                task.setStatus(abortType);
                TaskReport report = new TaskReport(task);
                Header reportHeader = new Header(HeaderType.TASK_REPORT, report);
                sendHeader(reportHeader);
            }
            task.setStatus(abortType);
            if (!runningTask.cancel(true)) {
                log.warning("failed to cancel running task");
            }
        } else {
            log.fine("told to abort but no task running.");
        }
        if (abortType == TaskStatus.LOCAL_ABORT) {
            try {
                taskHandler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                //nothing to do here..continue
            }
            signalShutdown();
            if (gruntForm != null) {
                gruntForm.exitNoQuery();
            }
        }
    }

    public Task getCurrentTask() {
        return task;
    }

    boolean isBusy() {
        return status == GruntStatus.BUSY;
    }

    void handleFatalException(Exception ex) {

        ErrorHelper.outputToLogMsgAndKill(gruntForm, gruntcl, log, "Fatal error. Click ok to exit.", ex);
        shutdown(false);
    }

    /**
     * user via AWT or local master - this is responsible for:
     * 1. setting localShutdown to true
     * 2. trying to close the gruntStreamSock (interrupts gruntreceiveThread)
     * if w/ localMaster, then we should abort task as well
     *
     */
    void signalShutdown() {
        localShutdown = true;   //flag for both receiver thread and task thread
        tryCloseMSock();    //just in case it wasn't closed after discovery
        if (gruntStreamSock != null) {
            gruntStreamSock.tryClose();
        }
        log.finest("signalShutdown()");
    }

    /**
     * Receive header object which specifies the action to take: get more files from the stream; pass task to taskPool.
     *
     * @param header
     * @return true if master is quitting, false otherwise
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @SuppressWarnings("unchecked")
    private boolean couldHandleDelivery(Header header) throws IOException {

        HeaderType type = header.getHeaderType();
        switch (type) {

            case TASK_ASSIGN:
                if (task != null) {
                    log.severe("received another task before first was done!");
                } else {    //we're ok
                    task = header.getTask();
                    AssignedTask myTask = new AssignedTask();
                    runningTask = new FutureTask<String>(myTask);
                    taskHandler.submit(runningTask);
                }
                break;
            case TASK_ABORT:
                abortCurrentTask(TaskStatus.MASTER_ABORT);
                break;
            case QUIT_AFTER_TASK:
                gruntQuitting = true;
                shutdown(task != null);
                break;
            case FILE_REPLY:
                status = GruntStatus.RECEIVING;
                receiveFile(header);
                status = GruntStatus.BUSY;
                AssignedTask myTask = new AssignedTask();
                runningTask = new FutureTask<String>(myTask);
                taskHandler.submit(runningTask);
                break;
            case MASTER_SHUTDOWN:
                log.finer("received notice that master is shutting down");
                return true;
            default:
                log.severe("couldHandleDelivery received an unknown Header type: " + type);
        }
        return false;
    }

    private void shutdown(boolean patient) {
        log.finest("entering shutdown -->");

        taskHandler.shutdown();
        if (machineUpdateHandler != null) {
            machineUpdateHandler.shutdown();
            try {
                machineUpdateHandler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                //nothing here
            }
            machineUpdateHandler.shutdownNow();
        }
        try {
            if (patient) {
                while (!taskHandler.isTerminated()) {
                    //patiently waiting...
                }
            } else {
                taskHandler.awaitTermination(1, TimeUnit.SECONDS);
            }

            taskHandler.shutdownNow();
            signalShutdown();
        } catch (InterruptedException ex) {
            //we're shutting down, so nothing to do here
            log.finest("grunt interrupted during shutdown - weird");
        }

        try {
            if (masterR == null) { //if we're NOT with localMaster, write to file
                Config.writeCfgToFile(config);
                IOHelper.deleteRunningLock();
            }
        } catch (IOException ex) {
            String msg = "failed to write to loki.cfg.  Check filesystem permissions.";

            if (gruntForm != null) {
                MasterEQCaller.showMessageDialog(gruntForm, "Error", msg, JOptionPane.WARNING_MESSAGE);
            } else {
                System.out.println(msg);
            }

            log.warning("failed to write config to file:" + ex.getMessage());
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            //squelch
        }

        if (gruntForm != null) {
            gruntForm.dispose();
        }

        if (masterR == null) {
            System.exit(0);
        }
    }

    /**
     *
     * @param header
     * @return
     * @throws IOException if our socket op failed/closed
     */
    private void receiveFile(Header header) throws IOException {

        long projectFileSize = header.getProjectFileSize();
        String projectFileName = header.getProjectFileName();

        updateGruntStatusUI(GruntTxtStatus.RECEIVING, projectFileSize);

        GruntIOHelper.receiveFileFromBroker(projectFileName,
                projectFileSize,
                config,
                gruntForm,
                gruntStreamSock);

        updateGruntStatusUI(GruntTxtStatus.BUSY);
    }

    /**
     * finds master's IP address, or gets from config if set manually
     * @return true if address was found, false if network failed, or
     * we received an interrupt(shutdown). if false then localShutdown = true
     */
    private boolean canGetMasterAddress() {

        if (config.isAutoDiscoverMaster()) {
            try {
                //throws Socket, IO exceptions
                DatagramPacket packet = listenForMaster();

                if (!localShutdown) {
                    config.setMasterIp(packet.getAddress());

                    String masterInfo = new String(packet.getData());
                    StringTokenizer stringTokenizer = new StringTokenizer(masterInfo, ";");
                    masterName = stringTokenizer.nextToken();
                    masterLokiVersion = stringTokenizer.nextToken();
                }

            } catch (IOException ex) {
                /**
                 * either we:
                 * 1. received a fatal IOE and should shutdown or
                 * 2. user set localShutdown = true, and closed port
                 * so in either case, we shutdown.
                 */
                if (!localShutdown) {
                    //if UI didn't signal, this was fatal IOE, and we should
                    //tell the user and try to close the socket

                    log.warning("Loki is unable to setup multicast\n" +
                            "to discover the master. Verify that your\n" +
                            "network is properly configured. (Hint:\n" +
                            "does your network have a default route?)");

                    handleFatalException(ex);
                    log.throwing(className, "findMaster()", ex);
                }
            }
        }

        return !localShutdown;
    }
    
    private DatagramPacket listenForMaster() throws SocketException, IOException {

        boolean packetReceived = false;
        byte[] buf = new byte[256];
        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);

        received:
        while (!packetReceived) {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                if (!networkInterface.isLoopback()) {

                    for (InetAddress inetAddress : Collections.list(addresses)) {
                        if (inetAddress instanceof Inet4Address) {
                            MulticastSocket multicastSocket = new MulticastSocket(config.getGruntMulticastPort());

                            //throws IOE
                            multicastSocket.setInterface(inetAddress);
                            multicastSocket.joinGroup(config.getMulticastAddress());
                            multicastSocket.setSoTimeout(1000);
                            try {
                                multicastSocket.receive(datagramPacket);  //blocks here until we get a packet
                                packetReceived = true;
                                multicastSocket.close();
                                break received;
                            } catch (SocketTimeoutException sockEx) {
                                //go again
                            }
                            if (localShutdown) {
                                break received;
                            }
                            multicastSocket.close();
                        }
                    }
                }
            }
        }
        return datagramPacket;
    }

    /**
     * closes the multicast socket if not already closed
     * called by the UI for a shutdown, or by receiverThread during findMaster
     * if we get a socket IOE
     */
    private void tryCloseMSock() {
        if (mSock != null) {
            if (!mSock.isClosed()) {
                mSock.close();
            }
        }
    }

    private void sendHeader(Header header) {
        try {
            gruntStreamSock.sendHeader(header);
        } catch (IOException ex) {
            /**
             * come here if:
             * 1. we lost socket because of other IOE
             * 2. user closed socket
             *
             */
            if (!localShutdown) {    //case 1
                gruntStreamSock.tryClose();
                log.throwing(className, "run()", ex);
            } else {
                log.fine("socket closed");
                //user shutdown so they already closed socket; do nothing
            }
        }
    }

    private void updateGruntStatusUI(GruntTxtStatus currentStatus) {
        if (gruntForm != null) {
            GruntEQCaller.invokeUpdateStatus(gruntForm, new GruntStatusText(currentStatus));
        } else {
            System.out.println(currentStatus.toString());
        }
    }

    private void updateGruntStatusUI(GruntTxtStatus currentStatus, long val) {
        if (gruntForm != null) {
            GruntEQCaller.invokeUpdateStatus(gruntForm, new GruntStatusText(currentStatus, val));
        } else {
            System.out.println(currentStatus.toString() + " (bytes) :" + Long.toString(val));
        }
    }

    /**
     * private inner class that handles a given assigned task
     */
    private class AssignedTask implements Callable {

        @Override
        public String call() {
            if (task == null) {
                log.severe("task is null!");
            } else {    //let's get to work
                status = GruntStatus.BUSY;

                TaskStatus status = task.getStatus();

                if (task.isAutoFileTranfer()) {
                    if (areFilesInCache()) {
                        if (status != TaskStatus.DONE && status != TaskStatus.FAILED) { //task hasn't run yet
                            try {
                                ConcurrentHashMap<String, ProjectFileObject> fileCacheMap = config.getGruntFileCacheMap();
                                fileCacheMap.get(task.getProjectFileName()).setInUse(true);
                                task.setStatus(TaskStatus.RUNNING);

                                runTaskWrapper();

                                fileCacheMap.get(task.getProjectFileName()).setInUse(false);
                                fileCacheMap.get(task.getProjectFileName()).updateTime();
                            } catch (IOException ex) {
                                log.warning(ex.getMessage());
                            }
                        }
                        TaskReport report = new TaskReport(task);

                        if (!gruntStreamSock.isClosed()) {
                            sendHeader(new Header(HeaderType.TASK_REPORT, report));

                            switch (task.getStatus()) {

                                case DONE:
                                    //if successful, send files too
                                    sendOutputFiles();
                                    log.finer("sent output files");
                                    log.finer("task set to null");
                                    task = null; //task done and sent, so set to null
                                    if (!gruntQuitting) {
                                        sendHeader(new Header(HeaderType.IDLE));
                                    }
                                    break;
                                case FAILED:
                                case MASTER_ABORT:
                                    task = null;    //ditch current task
                                    sendHeader(new Header(HeaderType.IDLE));
                                    break;
                                case LOCAL_ABORT:
                                    task = null;    //ditch current task
                                    signalShutdown();
                                    break;
                                case READY:
                                case RUNNING:
                                case LOST_GRUNT:
                            }
                        } else {
                            updateGruntStatusUI(GruntTxtStatus.PENDING_SEND);

                            log.fine("socket closed! will try and send next connect");
                        }
                    }
                } else {    //no file caching and transfer - network share

                    if (true) {
                        throw  new RuntimeException("case: no file caching and transfer - network share: is under work!");
                    }

                    if (status != TaskStatus.DONE && status != TaskStatus.FAILED) { //task hasn't run yet
                        try {
                            task.setStatus(TaskStatus.RUNNING);
                            runTaskWrapper();

                        } catch (IOException ex) {
                            log.warning(ex.getMessage());
                        }
                    }
                    TaskReport report = new TaskReport(task);

                    if (!gruntStreamSock.isClosed()) {
                        Header reportHeader = new Header(HeaderType.TASK_REPORT, report);
                        sendHeader(reportHeader);

                        switch (status) {

                            case DONE:
                                if (task.isAutoFileTranfer()) {
                                    //if successful, send a files too
                                    sendOutputFiles();
                                    log.finer("sent output file");
                                }
                                //task done (and sent if auto)set to null
                                task = null;
                                log.finer("task set to null");
                                sendHeader(new Header(HeaderType.IDLE));
                                break;
                            case FAILED:
                            case MASTER_ABORT:
                                task = null;    //ditch current task
                                sendHeader(new Header(HeaderType.IDLE));
                                break;
                            case LOCAL_ABORT:
                                task = null;    //ditch current task
                                signalShutdown();
                                break;
                            case READY:
                            case RUNNING:
                            case LOST_GRUNT:
                        }
                    } else {
                        updateGruntStatusUI(GruntTxtStatus.PENDING_SEND);
                        log.fine("socket closed! will try and send next connect");
                    }
                }
            }
            status = GruntStatus.IDLE;
            return "done";
        }

        private void runTaskWrapper() throws IOException {

            String[] taskCL = CommandLineHelper.generateFileTaskCommandLine(config.getBlenderBin(), task);
            String[] result = runTask(taskCL);

            task.setInitialOutput(taskCL, result[0], result[1]);
            if (task.getStatus() != TaskStatus.LOCAL_ABORT && task.getStatus() != TaskStatus.MASTER_ABORT) {
                task.determineStatus();
            }

            switch (task.getStatus()) {
                case DONE:
                    task.populateDoneInfo();
                    updateGruntStatusUI(GruntTxtStatus.IDLE);
                    break;
                case FAILED:
                    updateGruntStatusUI(GruntTxtStatus.ERROR);
                    log.warning("task failed with output: " + task.getStdout() + "\n" + task.getErrOut());
                    break;
                case READY:
                case RUNNING:
                case LOCAL_ABORT:
                case MASTER_ABORT:
                case LOST_GRUNT:
                    break;
                default:
                    log.warning("unknown task type in runTaskWrapper");
            }
        }

        private String[] runTask(String[] taskCL) {

            ProcessHelper processHelper = new ProcessHelper(taskCL);

            updateGruntStatusUI(GruntTxtStatus.BUSY);

            //block here until process returns
            String[] result = processHelper.runProcess();

            if (result[1].contains("IOException")) {
                updateGruntStatusUI(GruntTxtStatus.ERROR);

                if (result[1].contains("No such file or directory")) {
                    //task executable wasn't found
                    result[0] = "task executable not found (e.g. blender)!";
                    log.warning("task executable not found for task type:" +
                            task.getJobType());

                }
            } else if (result[1].contains("InterruptedException")) {
                updateGruntStatusUI(GruntTxtStatus.ABORT);

                if (task.getStatus() == TaskStatus.MASTER_ABORT) {
                    result[0] = "master aborted task";
                    log.info("task aborted by master");
                } else {    //local abort
                    task.setStatus(TaskStatus.LOCAL_ABORT);
                    result[0] = "local user aborted task";
                    log.info("task aborted by local user");

                }
            } else if (result[1].contains("ExecutionException")) {
                updateGruntStatusUI(GruntTxtStatus.ABORT);
            }
            return result;
        }

        private void sendOutputFiles() {

            List<File> files = getFilesToSend();

            long allFilesSize = GruntIOHelper.countAllFilesSize(files);
            updateGruntStatusUI(GruntTxtStatus.SEND, allFilesSize);

            GruntIOHelper.sendOutputFilesToBroker(gruntForm, files, gruntStreamSock, task.isTile(), allFilesSize);
            updateGruntStatusUI(GruntTxtStatus.IDLE);
        }

        private List<File> getFilesToSend() {
            List<File> files = new ArrayList<>();
            for (RenderedFileAttribute attribute : task.getRenderedFileAttributes()) {
                files.add(attribute.getFile());
            }
            return files;
        }

        /**
         * If file is not in cache, then request it
         * @return
         */
        private boolean areFilesInCache() {

            String projectFileName = task.getProjectFileName();

            if (!config.getGruntFileCacheMap().containsKey(projectFileName)) {
                sendHeader(new Header(HeaderType.FILE_REQUEST, projectFileName));
                return false;
            } else {
                sendHeader(new Header(HeaderType.BUSY));
            }
            return true;
        }
    }

    public static void setupWorkingGruntFolders(File gruntMainFolder, File cacheFolder, File renderFolder) {

        MAIN_FOLDER = gruntMainFolder;
        CACHE_FOLDER = cacheFolder;
        RENDER_FOLDER= renderFolder;
    }
}
