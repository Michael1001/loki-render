package net.whn.loki.master;

import net.whn.loki.brokersModel.BrokersModel;
import net.whn.loki.common.*;
import net.whn.loki.common.ProjectFileObject;
import net.whn.loki.common.configs.Config;
import net.whn.loki.error.ErrorHelper;
import net.whn.loki.error.LostGruntException;
import net.whn.loki.error.MasterFrozenException;
import net.whn.loki.grunt.GruntR;
import net.whn.loki.io.IOHelper;
import net.whn.loki.io.MasterIOHelper;
import net.whn.loki.messaging.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * This is the central control point for all job and grunt management
 */
public class MasterR extends MsgQueue implements Runnable, ICommon {

    private static final String className = MasterR.class.toString();
    private static final Logger log = Logger.getLogger(className);

    public static File MAIN_FOLDER;
    public static File CACHE_FOLDER;
    public static File TEMP_FOLDER;
    public static final String MAIN_FOLDER_NAME = "master";
    public static final String CACHE_FOLDER_NAME = "fileCache";
    /**
     * Used to create temp folder for the tile render, where to receive grunt's tiled images.
     */
    public static final String TEMP_FOLDER_NAME = "tempFolder";

    private final Config config;
    private volatile GruntR grunt;  //set by local grunt, read by master
    private final JobsModel jobsModel;  //holds all the jobs
    private final ConcurrentHashMap<String, ProjectFileObject> fileCacheMap;
    private final BrokersModel brokersModel;  //holds all the brokers
    private MasterForm masterForm;        //handle for masterForm
    private AnnouncerR announcer;
    private ListenerR listener;
    private Thread listenerThread, announcerThread;
    private final ExecutorService executorServiceCompositer;
    private ThreadGroup helpersThreadGroup;
    private ThreadGroup brokersThreadGroup;
    private volatile boolean queueRunning;  //volatile since UI changes this
    private boolean shutdown;
    private int lastTotalCores = -1;

    public MasterR(Config config, AnnouncerR announcerR, int mQSize) {

        super(mQSize);

        this.config = config;
        Job.setJobIDCounter(config.getJobIDCounter());
        Task.setTaskIDCounter(config.getTaskIDCounter());
        jobsModel = config.getJobsModel();
        fileCacheMap = config.getFileCacheMap();

        brokersModel = new BrokersModel(this, fileCacheMap);
        announcer = announcerR;

        //listener setup here
        try {
            listener = new ListenerR(config.getConnectPort(), config.getFilesReceivePort(), config.getUpdateMachinePort(),this);
        } catch (Exception ex) {
            //if either of these fail, we have to quit
            log.severe("listener failed to setup!");
            throw new IllegalThreadStateException();
        }

        helpersThreadGroup = new ThreadGroup("helpers");
        brokersThreadGroup = new ThreadGroup("brokers");

        listenerThread = new Thread(helpersThreadGroup, listener, "listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        announcerThread = new Thread(helpersThreadGroup, announcer, "announcer");
        announcerThread.setDaemon(true);
        announcerThread.start();

        executorServiceCompositer = Executors.newSingleThreadExecutor();
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                handleMessage(fetchNextMessage());
            } catch (InterruptedException IntEx) {
                /**
                 * signaled ourselves to shutdown
                 */
                break;
            } catch (IOException ex) {
                ErrorHelper.outputToLogMsgAndKill(masterForm, false, log, "Loki encountered an error", ex);
            } catch (MasterFrozenException mfe) {
                //impossible
            }
        }
    }

    /**
     * called by a local grunt (if one is present) right after startup
     * we need this so we can tell the local grunt to shutdown when master does
     * @param g
     */
    public void setGrunt(GruntR g) {
        grunt = g;
    }

    /**
     * @return the config object for this session
     */
    public Config getConfig() {
        return config;
    }

    /**
     * passes a handle of the masterForm object for master to use
     * @param mjForm
     */
    public void setMasterForm(MasterForm mjForm) {
        masterForm = mjForm;
        updateProgressBar();
    }

    /**
     * called by MasterForm (AWT) if user clicked stop
     */
    public void setQueueRunningFalse() {
        queueRunning = false;
    }

    /**
     * called by AWT for user exit request - are there any jobs running (tasks)
     * @return true if yes, false if none
     */
    boolean areJobsRunning() {
        return jobsModel.areJobsRunning();
    }

    boolean areJobsRunning(int[] selectedRows) {
        return jobsModel.areSelectedJobsRunning(selectedRows);
    }

    /**
     * used by masterForm to get handle on jobsModel
     * @return
     */
    public JobsModel getJobsModel() {
        return jobsModel;
    }

    /**
     * used by masterForm to get handle on gruntsModel
     * @return
     */
    BrokersModel getBrokersModel() {
        return brokersModel;
    }

    /**
     * main decision point for masterThread
     * @param message
     * @throws InterruptedException
     */
    private void handleMessage(Message message) throws InterruptedException, IOException, MasterFrozenException {

        MessageType type = message.getType();
        switch (type) {

            case SHUTDOWN:
                shutdown();
                break;
            case FATAL_ERROR:
                handleFatalError(message);
                break;
            case START_QUEUE:
                queueStarting();
                break;
            case ADD_JOB:
                addJob(message);
                break;
            case VIEW_JOB:
                viewJob(message);
                break;
            case REMOVE_JOBS:
                removeJobs(message);
                break;
            case ABORT_ALL:
                abortAll_StopQueue();
                break;
            case ADD_GRUNT:
                addGrunt(message);
                break;
            case UPDATE_GRUNT:
                updateGrunt(message);
                break;
            case REMOVE_GRUNT:
                removeGrunt(message);
                break;
            case IDLE_GRUNT:
                idleGrunt(message);
                break;
            case VIEW_GRUNT:
                viewGrunt(message);
                break;
            case QUIT_GRUNT:
                quitGrunt(message);
                break;
            case LOST_BUSY_GRUNT:
                handleLostBusyGrunt(message);
                break;
            case QUIT_ALL_GRUNTS:
                quitAllGrunts();
                break;
            case TASK_REPORT:
                handleReport(message);
                break;
            case RESET_FAILURES:
                resetFailures(message);
                break;
            case FILE_REQUEST:
                brokersModel.handleFileRequest(message);
                break;
            case FILE_RECEIVED:
                FileReceivedMessage fileReceivedMsg = (FileReceivedMessage) message;
                long gruntID = ((FileReceivedMessage) message).getGruntID();
                break;
            default:
                log.warning("master received unknown MessageType: " + type);
        }
    }

    /**
     * calls shutdown then throws up the stack
     * @param m
     */
    private void handleFatalError(Message m) {
        FatalThrowableMessage fatalMsg = (FatalThrowableMessage) m;
        Throwable throwable = fatalMsg.getThrowable();

        ErrorHelper.outputToLogMsgAndKill(masterForm, false, log,
                "Loki encountered a fatal error.\n" +
                "Click OK to exit.", throwable);

        shutdown();
    }

    /**
     * passes new job to jobsModel
     * called by addJob in masterForm
     * @param message
     */
    private void addJob(Message message) {

        final AddJobMessage addJobMessage = (AddJobMessage) message;
        JobFormInput newJobInput = addJobMessage.getJobFormInput();

        File projectFile = new File(newJobInput.getProjectFilePath());

        if (newJobInput.isAutoFileTransfer()) {
            MasterIOHelper.addProjectFileToCache(fileCacheMap, projectFile, config, CACHE_FOLDER);
        }

        jobsModel.addJob(new Job(newJobInput, projectFile, config));
        updateProgressBar();
        addJobMessage.disposeAddingJobForm();
    }

    private void resetFailures(Message m) {
        final ResetFailuresMessage msg = (ResetFailuresMessage) m;
        jobsModel.resetFailures(msg.getRows());
    }

    /**
     * removes one or more jobs as selected by user in job table
     * @param m
     */
    private void removeJobs(Message m) throws IOException, InterruptedException, MasterFrozenException {
        final RemoveJobsMessage message = (RemoveJobsMessage) m;
        //get gruntIDs and set tasks back to READY here:
        ArrayList<Long> gruntIDs = jobsModel.getGruntIDsForSelectedRunningJobs(message.getRowsToRemove());
        abortGrunts(gruntIDs);

        //now finally remove jobs
        jobsModel.removeJobs(message.getRowsToRemove());
        updateProgressBar();
    }

    private void abortAll_StopQueue() throws IOException, InterruptedException, MasterFrozenException {
        queueRunning = false;
        masterForm.stopQueue();
        ArrayList<Long> gruntIDs = jobsModel.getGruntIDsForAllRunningTasks();
        abortGrunts(gruntIDs);
    }

    private void abortGrunts(ArrayList<Long> gruntIDs) throws InterruptedException, IOException, MasterFrozenException {
        //tell the grunts to abort the tasks
        brokersModel.abortTasksForGrunts(gruntIDs);
    }

    /**
     * update main progress bar (tasks) on masterForm
     */
    private void updateProgressBar() {
        MasterEQCaller.invokeUpdatePBar(masterForm, jobsModel.getProgressBarUpdate());
    }

    /**
     * update totalcore count on masterForm if it's different then last update
     * this saves us unecessary EQ calls in this particular case
     */
    private void updateCoresDisplay() {
        int total = brokersModel.getCores();
        if (total != lastTotalCores) {
            MasterEQCaller.invokeUpdateCores(masterForm, total);
        }
    }

    /**
     * listener picked up a new grunt on the accept port, so let's
     * create a broker for it and add it to gruntsModel
     * @param message
     */
    private void addGrunt(Message message) {
        try {
            AddGruntMessage addGruntMessage = (AddGruntMessage) message; //cast

            Socket gruntSocket = addGruntMessage.getGruntSocket();
            Socket fileReceiveSocket = addGruntMessage.getFileReceiveSocket();
            Socket updateMachineSocket = addGruntMessage.getUpdateMachineSocket();

            brokersModel.addGrunt(gruntSocket, fileReceiveSocket, updateMachineSocket, brokersThreadGroup);
        } catch (IOException ex) {
            //we hadn't added to gruntsModel when ex thrown, so just log...
            log.throwing(className, "addGrunt(Message message)", ex);
        }
    }

    /**
     * Original message sent by user from MasterForm
     */
    private void shutdown() {
        queueRunning = false;
        masterForm.stopQueue();
        if (grunt != null) {
            Task task = grunt.getCurrentTask();
            if (task != null) {
                jobsModel.setTaskStatus(task.getJobId(), task.getId(), TaskStatus.READY);
            }
            grunt.abortCurrentTask(TaskStatus.LOCAL_ABORT);
        }

        //notify announcer and listener threads.
        helpersThreadGroup.interrupt();

        //first, send shutdown messages to each broker
        brokersModel.shutdown();

        //signal executorServiceCompositer to shutdown
        executorServiceCompositer.shutdownNow();

        try {
            //first put latest info into config object
            config.setMasterCfg(Job.getJobIDCounter(), Task.getTaskIDCounter());

            //now write it to file
            Config.writeCfgToFile(config);

            log.finest("writing config to file, as always.");

            //wait up to 1 second for listenerThread
            listenerThread.join(1000);

        } catch (IOException ex) {
            MasterEQCaller.showMessageDialog(masterForm, "filesystem error",
                    "failed to write to loki.cfg.\n" +
                    "Check filesystem permissions.",
                    JOptionPane.WARNING_MESSAGE);
            log.warning("failed to write cfg to file:" + ex.getMessage());
        } catch (InterruptedException ex) {
            //do nothing...we're exiting
        }
        IOHelper.deleteRunningLock();

        //finally, set shutdown to true so I exit main loop
        shutdown = true;
        try {
        Thread.sleep(1000); //make sure local grunt has time to shutdown
        } catch (InterruptedException ex) {
            //squelch...
        }
        System.exit(0);
    }

    /**
     * refresh the UI w/ the latest info for grunt x; also update cores
     * in case that changed
     * @param m
     */
    private void updateGrunt(Message m) {
        log.finest("updating grunt");
        UpdateGruntMessage message = (UpdateGruntMessage) m;
        brokersModel.updateGruntRow(message.getGruntID());
        if (message.isFirstMachineReply()) {
            updateCoresDisplay();
        }
    }

    /**
     * grunt x notified us that it is idle
     * @param m
     * @throws InterruptedException
     */
    private void idleGrunt(Message m) throws InterruptedException {
        IdleGruntMessage iMsg = (IdleGruntMessage) m;

        //set grunt's status back to idle
        brokersModel.setGruntStatusToIdle(iMsg.getGruntID());

        //give out next task if possible
        try {
            assignIdleGrunts();
        } catch (LostGruntException ex) {   //kill the lost grunt!
            log.fine("lost grunt with id: " + ex.getGruntID());
            try {
                deliverMessage(new RemoveGruntMessage(ex.getGruntID())); //notify myself
            } catch (MasterFrozenException mfe) {
                //impossible...will never detect it's own freezeup
            }
        }
    }

    /**
     * remove grunt from gruntsModel; update cores on UI
     * @param message
     */
    private void removeGrunt(Message message) {
        RemoveGruntMessage removeGruntMessage = (RemoveGruntMessage) message;
        switch (removeGruntMessage.getGruntStatus()) {

            case ERROR:
                break;
            case BUSY:
                Task task = removeGruntMessage.getGruntTask();
                task.setGruntId(-1);   //we're losing the grunt association w/ task
            case IDLE:
                brokersModel.removeGrunt(removeGruntMessage.getGruntID());
                updateCoresDisplay();
        }
    }

    /**
     * view grunt details
     */
    private void viewGrunt(Message m) {
        SelectedGruntMessage message = (SelectedGruntMessage) m;
        GruntDetails details = brokersModel.getGruntDetails(message.getRow());

        MasterEQCaller.invokeViewGruntDetails(masterForm, details);
    }

    private void quitGrunt(Message m) throws MasterFrozenException {
        SelectedGruntMessage message = (SelectedGruntMessage) m;
        brokersModel.quitGrunt(message.getRow());
    }

    private void quitAllGrunts() throws MasterFrozenException {
        brokersModel.quitAllGrunts();
    }

    private void viewJob(Message m) {
        SelectedGruntMessage message = (SelectedGruntMessage) m;
        try {
            Job job = jobsModel.getJobDetails(message.getRow());
            MasterEQCaller.invokeViewJobDetails(masterForm, job);
        } catch (IOException ex) {
            log.severe("IOEx while trying to clone Job: " + ex.getMessage());
        } catch (ClassNotFoundException cex) {
            log.severe("failed trying to clone Job: " + cex.getMessage());
        }
    }

    /**
     *
     *this method should be called in 3 cases:
     * 1. user has pressed the start button
     * 2. a new (idle) grunt has connected
     * 3. a grunt is done with a task and now idle
     * 
     * @throws InterruptedException if inter-thread messaging times out
     */
    private void assignIdleGrunts() throws LostGruntException {
        boolean tryNextTask = true;
        while (tryNextTask && queueRunning) {
            Task nextTask = jobsModel.getNextTask();   //try and get next task
            if (nextTask != null) { // is null if we couldn't find one
                if (brokersModel.sendNextTask(nextTask)) {

                    //update the task status in the job
                    jobsModel.setTaskStatus(nextTask.getJobId(), nextTask.getId(), TaskStatus.RUNNING);
                } else {
                    tryNextTask = false;
                }
            } else { //last iteration failed to find any remaining tasks
                tryNextTask = false;
                if (jobsModel.areAllJobsDone()) { //all status ALL_TASKS_FINISHED_OR_ABORTED?
                    MasterEQCaller.invokeStop(masterForm);
                }
            }
        }
    }

    /**
     * called as a result of receiving a 'startQueue' message from UI
     * @throws InterruptedException if MQDelivery failed
     */
    private void queueStarting() throws InterruptedException {
        queueRunning = true;
        //give out next task if possible
        try {
            assignIdleGrunts();
        } catch (LostGruntException ex) {   //kill the lost grunt!
            log.fine("lost grunt with id: " + ex.getGruntID());
            try {
                deliverMessage(new RemoveGruntMessage(ex.getGruntID())); //notify myself
            } catch (MasterFrozenException mfe) {
                //impossible
            }
        }
    }

    private void compositeTiles(Task task) {

        CompositeTiles compositeTiles = new CompositeTiles(getTileFolder(task), task);
        executorServiceCompositer.submit(compositeTiles);
    }

    private File getTileFolder(Task task) {
        return new File(TEMP_FOLDER, task.getJobId() + "-" + task.getFrame());
    }

    //call when we lose contact with a busy grunt
    // - assume task is lost and reset given task to 'ready'
    private void handleLostBusyGrunt(Message message) {

      LostBusyGruntMessage lostBusyGruntMsg = (LostBusyGruntMessage) message;
      Task task = lostBusyGruntMsg.getGruntTask();
      jobsModel.setTaskStatus(task.getJobId(), task.getId(), TaskStatus.READY);
    }

    /**
     * called when a grunt is done/failed with a task
     * @param message
     * @throws InterruptedException if delivery timed out
     */
    private void handleReport(Message message) {
        TaskReportMessage reportMsg = (TaskReportMessage) message;    //cast back
        TaskReport taskReport = reportMsg.getReport();

        if (taskReport.getTask().getStatus() == TaskStatus.DONE) {
            //update the job w/ info reported
            if (jobsModel.setReturnTask(taskReport)) {
                compositeTiles(taskReport.getTask());
            }
            updateProgressBar();

        } else if (taskReport.getTask().getStatus() == TaskStatus.FAILED) {
            //task failed
            Task t = taskReport.getTask();
            jobsModel.setReturnTask(taskReport);
            String failureMsg;
            if(t.getStdout().length() > 0) {
                failureMsg = t.getStdout();
            } else if (t.getErrOut().length() > 0) {
                failureMsg = t.getErrOut();
            } else
                failureMsg = "unknown";

            String failed = "Task failed for grunt '" +
                    brokersModel.getGruntName(t.getGruntId()) +
                    "' with the message:\n\"" + failureMsg + "\"";



            MasterEQCaller.invokeTaskFailureNotification(masterForm, failureMsg);
            log.warning(failed);

        } else if (taskReport.getTask().getStatus() == TaskStatus.LOCAL_ABORT) {
            Task t = taskReport.getTask();
            jobsModel.setTaskStatus(t.getJobId(), t.getId(),
                    TaskStatus.READY);
            String aborted = "User aborted task and quit Loki on grunt '" +
                    brokersModel.getGruntName(t.getGruntId()) + "'.";
            MasterEQCaller.showMessageDialog(masterForm, "task aborted",
                    aborted, JOptionPane.INFORMATION_MESSAGE);

        } else if (taskReport.getTask().getStatus() == TaskStatus.MASTER_ABORT) {
            Task t = taskReport.getTask();
            jobsModel.setTaskStatus(t.getJobId(), t.getId(),
                    TaskStatus.READY);
        } else {
            log.severe("don't know how to handle tasktype: " +
                    taskReport.getTask().getStatus());
        }
    }

    public static void setupWorkingMasterFolders(File mainFolder, File cacheFolder, File tempFolder) {

        MAIN_FOLDER = mainFolder;
        CACHE_FOLDER = cacheFolder;
        TEMP_FOLDER = tempFolder;
    }

    private class CompositeTiles implements Runnable {

        private Task task;
        private File tileFolder;

        CompositeTiles(File tileFolder, Task task) {
            this.tileFolder = tileFolder;
            this.task = task;
        }

        @Override
        public void run() {
            ImageHelper.compositeTiles(tileFolder, task);
        }
    }
}
