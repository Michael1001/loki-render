package net.whn.loki.brokersModel;

import net.whn.loki.common.GruntDetails;
import net.whn.loki.common.ICommon;
import net.whn.loki.common.ProjectFileObject;
import net.whn.loki.common.Task;
import net.whn.loki.error.ErrorHelper;
import net.whn.loki.error.LostGruntException;
import net.whn.loki.error.MasterFrozenException;
import net.whn.loki.master.MasterR;
import net.whn.loki.messaging.FileRequestMessage;
import net.whn.loki.messaging.Message;
import net.whn.loki.messaging.RemoveGruntMessage;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class BrokersModel extends AbstractTableModel implements ICommon {

    private static final String className = BrokersModel.class.toString();
    private static final Logger log = Logger.getLogger(className);

    private final MasterR masterR;
    private final List<Broker> brokersList = Collections.synchronizedList(new ArrayList<Broker>());
    private final ConcurrentHashMap<String, ProjectFileObject> fileCacheMap;
    private final String[] columnHeaders = getColumnHeaders();
    private ExecutorService fileSendPool;//for sending files to grunts
    //the master has better things to do

    public BrokersModel(MasterR masterR, ConcurrentHashMap<String, ProjectFileObject> fileCacheMap) {

        this.masterR = masterR;
        this.fileCacheMap = fileCacheMap;
        fileSendPool = Executors.newFixedThreadPool(2);
    }

    private String[] getColumnHeaders() {
        return new String[] {
                "name",
                "OS",
                "cores",
                "CPU %",
                "memory",
                "last task",
                "status"
        };
    }

    /**
     * AWT
     * @param index
     * @return column name
     */
    @Override
    public String getColumnName(int index) {
        return columnHeaders[index];
    }

    /**
     * AWT
     * @return
     */
    @Override
    public int getColumnCount() {
        return columnHeaders.length;
    }

    /**
     * returns the current row count of the model.
     * AWT
     * @return
     */
    @Override
    public int getRowCount() {
        return brokersList.size();
    }

    /**
     * fetches the column value on specified row (job).
     * AWT
     * @param row
     * @param column
     * @return string value
     */
    @Override
    public Object getValueAt(int row, int column) {
        return row < brokersList.size() ? brokersList.get(row).getValue(column) : "";
    }

    public void handleFileRequest(Message message) {
        FileRequestMessage fileRequestMsg = (FileRequestMessage) message;
        fileSendPool.submit(new FileToGruntTask(fileRequestMsg.getProjectFileName(), fileRequestMsg.getGruntId()));
    }

    public int getCores() {
        int cores = 0;
        synchronized (brokersList) {
            for (Broker broker : brokersList) {
                cores += broker.getCoreCount();
            }
        }
        return cores;
    }

    public void abortTasksForGrunts(ArrayList<Long> gruntIDs) throws IOException, MasterFrozenException {
        for (Long gruntId : gruntIDs) {
            try {
                int brokerRow = getBrokerIndex(gruntId);
                if (brokerRow != -1) {
                    brokersList.get(brokerRow).sendTaskAbort();
                }
            } catch (IOException ex) {
                log.info("failed on send to grunt");
                try {
                    masterR.deliverMessage(new RemoveGruntMessage(gruntId));
                } catch (InterruptedException iex) {
                    log.severe("failed to deliver msg to master!");
                }
            }
        }
    }

    public void addGrunt(Socket gruntSocket, Socket fileReceiveSocket, Socket updateMachineSocket, ThreadGroup brokersThreadGroup) throws IOException {

        Broker broker = new Broker(gruntSocket, fileReceiveSocket, updateMachineSocket, masterR, this);
        Thread brokerThread = new Thread(brokersThreadGroup, broker, "broker " + broker.getGruntID());
        broker.setThread(brokerThread);
        brokerThread.start();

        brokersList.add(broker);
        int newRow = brokersList.size() - 1;
        fireTableRowsInserted(newRow, newRow); //tell AWT EQ
    }

    /**
     * called by master
     */
    public void shutdown() {
        synchronized (brokersList) {
            for (Broker broker : brokersList) {
                try {
                    broker.shutdown();
                } catch (IOException ex) {
                    log.throwing(className, "shutdownAllGrunts()", ex);
                }
            }
        }
        fileSendPool.shutdownNow();
    }

    /**
     * if we have any changes to the grunt, we want to see the updates in
     * the table
     * run: master
     * @param gID
     */
    public void updateGruntRow(long gID) {
        int row = getBrokerIndex(gID);
        fireTableRowsUpdated(row, row); //tell AWT.EventQueue
    }

    /**
     * called by master to set grunt status
     * @param gID
     */
    public void setGruntStatusToIdle(long gID) {
        int index = getBrokerIndex(gID);
        if (index != -1) {
            brokersList.get(index).setGruntStatus(GruntStatus.IDLE);
            fireTableRowsUpdated(index, index);
        }
    }

    public void removeGrunt(long gruntID) {
        int brokerRow = getBrokerIndex(gruntID);
        if (brokerRow != -1) {
            brokersList.remove(brokerRow);
            fireTableRowsDeleted(brokerRow, brokerRow);
        }
    }

    public String getGruntName(long gID) {
        String name = "";
        int brokerRow = getBrokerIndex(gID);
        if (brokerRow != -1) {
            name = brokersList.get(brokerRow).getValue(0).toString();
        }
        return name;
    }

    public GruntDetails getGruntDetails(int row) {
        GruntDetails details = null;
        if (row < brokersList.size()) {
            details = brokersList.get(row).getDetails();
        }
        return details;
    }

    public void quitGrunt(int gruntRow) throws MasterFrozenException {
        try {
            if (gruntRow < brokersList.size()) {
                brokersList.get(gruntRow).sendQuit();
                updateBrokerRow(gruntRow, "last");
            }

        } catch (IOException ex) {
            log.info("failed on send to grunt");
            try {
                masterR.deliverMessage(new RemoveGruntMessage(brokersList.get(gruntRow).getGruntID()));
            } catch (InterruptedException iex) {
                log.severe("failed to deliver msg to master!");
            }
        }
    }

    public void quitAllGrunts() throws MasterFrozenException {
        synchronized (brokersList) {
            for (Broker broker : brokersList) {
                try {
                    broker.sendQuit();
                    updateBrokerRow(broker.getGruntID(), "last");
                } catch (IOException ex) {
                    log.info("failed on send to grunt");
                    try {
                        masterR.deliverMessage(new RemoveGruntMessage(broker.getGruntID()));
                    } catch (InterruptedException iex) {
                        log.severe("failed to deliver msg to master!");
                    }
                }
            }
        }
    }

    /**
     * Finds the next idle grunt and delivers task to it.
     *
     * @param task
     * @return true if it successfully delivered task to an idle grunt; false if there were no idle grunts
     * @throws LostGruntException when send on socket fails, in which case we
     *                            exit this method - broker should be killed higher up
     */
    public boolean sendNextTask(Task task) throws LostGruntException {

        synchronized (brokersList) {
            for (int row = 0; row < brokersList.size(); row++) {
                Broker broker = brokersList.get(row);
                if (broker.getGruntStatus() == GruntStatus.IDLE) {
                    try {
                        broker.sendTaskAssign(task);
                        task.setGruntId(broker.getGruntID());
                        broker.setGruntStatus(GruntStatus.RECEIVING);
                        fireTableRowsUpdated(row, row);
                        return true;
                    } catch (IOException ex) {
                        if (!broker.isSocketClosed()) {
                            //we're first to hit this problem, so log it
                            log.throwing(className, "sendNextTask(Task task)", ex);
                        }
                        throw new LostGruntException(broker.getGruntID());
                    }
                }
            }
        }
        return false;
    }

    /**
     * update grunt row in GUI w/ given status string
     * run: master
     * @param gID
     * @param newStatus
     */
    void updateBrokerRow(long gID, String newStatus) {
        int row = getBrokerIndex(gID);
        if (row != -1) {
            brokersList.get(row).setGruntStatusString(newStatus);
            fireTableRowsUpdated(row, row);
        }
    }

    /**
     * @param gruntId
     * @return the row of the broker for gruntID, -1 if it doesn't exist
     */
    private int getBrokerIndex(long gruntId) {

        synchronized (brokersList) {
            for (int i = 0; i < brokersList.size(); i++) {
                if (gruntId == brokersList.get(i).getGruntID()) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * private inner class that sends file to grunt via the broker socket
     * run: fileSendPool
     */
    private class FileToGruntTask implements Runnable {

        private final int gruntIndex;
        private ProjectFileObject projectFileObject;
        private final String projectFileName;

        FileToGruntTask(String projectFileName, long gruntId) {
            this.projectFileName = projectFileName;
            gruntIndex = getBrokerIndex(gruntId);
        }

        @Override
        public void run() {
            if (!fileCacheMap.containsKey(projectFileName)) {
                log.severe("grunt requested a file I don't have: " + projectFileName);
            } else {
                projectFileObject = fileCacheMap.get(projectFileName);
                if (!projectFileObject.getFile().exists()) {
                    log.severe("file for key " + projectFileName + " doesn't exist!");
                } else {
                    Broker broker = brokersList.get(gruntIndex);
                    try {
                        projectFileObject.updateTime();
                        broker.sendFile(projectFileObject);
                    } catch (IOException ex) {
                        log.warning("IO problem during file send: " + ex.getMessage());
                        try {
                            broker.sendRemoveGruntMessage();
                        } catch (InterruptedException | MasterFrozenException e) {
                            ErrorHelper.outputToLogMsgAndKill(null, false, log, "fatal error. exiting.", e);
                        }
                    }
                }
            }
        }
    }
}
