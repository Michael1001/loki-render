package net.whn.loki.master;

import net.whn.loki.common.*;
import net.whn.loki.common.configs.Config;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Represents a job in the queue
 */
public class Job implements ICommon, Serializable {

    private static final String className = "net.whn.loki.master.Job";
    private static final Logger log = Logger.getLogger(className);
    private static long jobIDCounter = 0;
    final private long jobId;
    private final String runnableBenderFileName;
    private JobType jobType;
    private final int firstFrame;
    private final int lastFrame;
    private final int totalFrames;
    private final String name;
    private final File projectFile;
    private final String projectFileName;
    private final File outputFolder;   //broker saves output here, master verifies
    private final String filePrefix;    //broker uses this
    private final boolean autoFileTransfer;
    private int readyTasks, runningTasks, doneTasks, failedTasks;
    private final boolean tileRender;
    private final int tileMultiplier;
    private JobStatus status;
    private Task[] tasks;
    private Integer renderingStep;
    private final boolean isEnabledAutoRunScripts;

    public Job(JobFormInput jobFormInput, File projectFile, Config config) {

        renderingStep = jobFormInput.getRenderigStep();

        FileExtensions selectedProjectFileExtension = jobFormInput.getSelectedProjectFileExtension();
        runnableBenderFileName = jobFormInput.getRunnableBenderFileName();

        jobId = jobIDCounter++;
        if (jobFormInput.getTaskType().equalsIgnoreCase(JobType.BLENDER.toString())) {
            jobType = JobType.BLENDER;
        }
        name = jobFormInput.getJobName();
        String outputFolderName = jobFormInput.getOutputFolderName();
        outputFolder = new File(outputFolderName);
        this.projectFile = projectFile;
        this.projectFileName = projectFile.getName();
        String projectFileNameWithoutExtension = FilenameUtils.removeExtension(projectFileName);
        filePrefix = jobFormInput.getFilePrefix();
        autoFileTransfer = jobFormInput.isAutoFileTransfer();

        resetFlags();
        status = JobStatus.REMAINING_AND_STOPPED;

        tileRender = jobFormInput.isTileEnabled();

        // get value from cb
        isEnabledAutoRunScripts = jobFormInput.isEnabledAutoRunScripts();
        boolean isEnabledCommandLineScripts = config.isEnabledCommandLineScripts();

        firstFrame = jobFormInput.getFirstFrame();
        lastFrame = jobFormInput.getLastFrame();

        int renderigStep = jobFormInput.getRenderigStep();
        int totalFramesWithoutStepping = (lastFrame - firstFrame) + 1;

        int result = totalFramesWithoutStepping / renderigStep;
        totalFrames = result == 0 ? 1 : result + (totalFramesWithoutStepping % renderigStep > 0 ? 1 : 0);


        if (tileRender) {
            tileMultiplier = jobFormInput.getTileMultiplier();
            int tilesPerFrame = tileMultiplier * tileMultiplier;

            tasks = new Task[totalFrames * tilesPerFrame];
            readyTasks = tasks.length;

            TileBorder[] tileBorders = calcTileBorders(tileMultiplier);

            int i = 0;
            for (int frame = firstFrame; frame <= lastFrame; frame += renderigStep) {
                for (int tile = 0; tile < tilesPerFrame; tile++) {
                    tasks[i++] = new Task(jobType,
                            frame,
                            jobId,
                            outputFolderName,
                            filePrefix,
                            projectFile,
                            projectFileNameWithoutExtension,
                            selectedProjectFileExtension,
                            autoFileTransfer,
                            true,
                            tile,
                            tilesPerFrame,
                            tileBorders[tile],
                            jobFormInput.isKeepingFoldersStructure(),
                            isEnabledAutoRunScripts,
                            isEnabledCommandLineScripts,
                            jobFormInput.getCommandLineScriptArgs(),
                            tileMultiplier,
                            runnableBenderFileName
                    );
                }
            }
        } else {    //not tile rendering
            tileMultiplier = -1;

            tasks = new Task[totalFrames];
            readyTasks = tasks.length;    //initially, all tasks are READY

            int i = 0;
            for (int frame = firstFrame; frame <= lastFrame; frame += renderigStep) {
                tasks[i++] = new Task(jobType,
                        frame,
                        jobId,
                        outputFolderName,
                        filePrefix,
                        projectFile,
                        projectFileNameWithoutExtension,
                        selectedProjectFileExtension,
                        autoFileTransfer,
                        jobFormInput.isKeepingFoldersStructure(),
                        isEnabledAutoRunScripts,
                        isEnabledCommandLineScripts,
                        jobFormInput.getCommandLineScriptArgs(),
                        runnableBenderFileName
                );
            }
        }
    }

    private void resetFlags() {
        runningTasks = 0;
        doneTasks = 0;
        failedTasks = 0;
    }

    /**
     * this is for the job details form
     * @return
     */
    public String getTileStr() {
        if (tileRender) {
            String m = Integer.toString(tileMultiplier);
            return m + " * " + m + " = " + Integer.toString(tileMultiplier * tileMultiplier);
        } else {
            return "disabled";
        }
    }

    /**
     * this method grabs value specified by column
     * called by jobsModel for the GUI table when we have an update fired to AWT
     * @param column
     * @return
     */
    public Object getValue(int column) {
        if (column == 0) {
            return name;
        } else if (column == 1) {
            return Integer.toString(failedTasks);
        } else if (column == 2) {
            return Integer.toString(readyTasks);
        } else if (column == 3) {
            return Integer.toString(runningTasks);
        } else if (column == 4) {
            return Integer.toString(doneTasks);
        } else if (column == 5) {
            if (status == JobStatus.REMAINING_AND_STOPPED) {
                return "ready";
            } else if (status == JobStatus.REMAINING_AND_TASKS_RUNNING || status == JobStatus.ALL_ASSIGNED_AND_RUNNING) {
                return "running";
            } else if (status == JobStatus.ALL_TASKS_FINISHED_OR_ABORTED) {
                return "done";
            } else {
                log.severe("An unknown jobStatus value was encountered: " +
                        status);
                return null;
            }
        } else {
            log.severe("A value outside of table scope was requested: " +
                    Integer.toString(column));
            return null;
        }
    }

    /**
     * just for test
     * @return true if counters match, false if there is a discrepancy
     */
    public boolean test_tallyCounters() {
        int rdy = 0;
        int rnng = 0;
        int dn = 0;
        int fld = 0;

        for (Task t : tasks) {
            if (t.getStatus() == TaskStatus.READY) {
                rdy++;
            } else if (t.getStatus() == TaskStatus.RUNNING) {
                rnng++;
            } else if (t.getStatus() == TaskStatus.DONE) {
                dn++;
            } else if (t.getStatus() == TaskStatus.FAILED) {
                fld++;
            } else {
                log.severe("status outside of scope: " + t.getStatus());
            }
        }
        if (rdy != readyTasks) {
            return false;
        }
        if (rnng != runningTasks) {
            return false;
        }
        if (dn != doneTasks) {
            return false;
        }
        if (fld != failedTasks) {
            return false;
        }

        return true;
    }

    /**
     * just for test!
     * @param task
     * @return
     */
    Task test_returnTask(int task) {
        return tasks[getTaskIndex(task)];
    }
    
    public boolean isAutoFileTransfer() {
        return autoFileTransfer;
    }

    /**
     * called by Master once on shutdown to get current idcounter
     * @return the current job id counter
     */
    public static long getJobIDCounter() {
        return jobIDCounter;
    }

    /**
     * called by Master once on startup if we're loading from a cfg file
     * @param jCounter
     */
    public static void setJobIDCounter(long jCounter) {
        jobIDCounter = jCounter;
    }

    public File getProjectFile() {
        return projectFile;
    }

    public String getProjectFileName() {
        return projectFileName;
    }

    /*BEGIN PACKAGE*/
    String getJobName() {
        return name;
    }

    long getJobId() {
        return jobId;
    }

    JobStatus getStatus() {
        return status;
    }

    void resetFailures() {
        for (Task t : tasks) {
            if (t.getStatus() == TaskStatus.FAILED) {
                setTaskStatus(t.getId(), TaskStatus.READY);
                t.setInitialOutput(null, null, null);
            }
        }
    }

    /**
     * called by getNextTask() in JobsModel; ancestor call is in MasterR
     * assignIdleGrunts();
     * finds the next task that is ready
     * @return
     */
    Task getNextAvailableTask() {
        for (Task task : tasks) {
            if (task.getStatus() == TaskStatus.READY) {
                return task;
            }
        }
        return null;
    }

    /**
     * update task w/ returned values
     * @param taskReport
     * @return
     */
    TaskStatus setReturnTask(TaskReport taskReport) {
        Task task = taskReport.getTask();
        int taskIndex = getTaskIndex(task.getId());
        //must set status first, before dropping in new object!
        setTaskStatus(task.getId(), task.getStatus());  //to update counters, not status
        tasks[taskIndex] = task;  //put the whole object in, rather than copying vars
        return tasks[taskIndex].getStatus();
    }

    /**
     * get gruntIDs for all grunts running tasks from this job
     * @param gIDList
     * @return
     */
    ArrayList<Long> getGruntIDsForRunningTasks(ArrayList<Long> gIDList) {
        for (Task t : tasks) {
            if (t.getStatus() == TaskStatus.RUNNING) {
                if (t.getGruntId() == -1) {
                    log.severe("running task has no associated grunt!");
                }
                gIDList.add(t.getGruntId());
                //setTaskStatus(t.getId(), TaskStatus.READY);
            }
        }
        return gIDList;
    }

    /**
     * sets the specified task's status
     * @param taskId
     * @param taskStatus
     * @throws IndexOutOfBoundsException
     */
    void setTaskStatus(long taskId, TaskStatus taskStatus) throws IndexOutOfBoundsException {

        int taskIndex = getTaskIndex(taskId);
        tasks[taskIndex].setStatus(taskStatus);
        recalculateJobStatus();
    }

    JobType getJobType() {
        return jobType;
    }

    int getTotalTasks() {
        return tasks.length;
    }

    int getRemainingTasks() {
        return tasks.length - doneTasks;
    }

    File getOutputFolder() {
        return outputFolder;
    }

    String getFilePrefix() {
        return filePrefix;
    }

    int getFirstTask() {
        return firstFrame;
    }

    int getLastTask() {
        return lastFrame;
    }

    int getDoneTasks() {
        return doneTasks;
    }

    int getFailedTasks() {
        return failedTasks;
    }

    int getReadyTasks() {
        return readyTasks;
    }

    int getRunningTasks() {
        return runningTasks;
    }

    Task[] getTasks() {
        return tasks;
    }

    /**
     *
     * @param multiplier
     * @return an array of tileBorders representing all tiles for given
     * multiplier
     */
    private TileBorder[] calcTileBorders(int multiplier) {
        TileBorder[] tileBorders = new TileBorder[multiplier * multiplier];
        float chunk = (float) 1 / (float) multiplier;

        int t = 0;
        float left, bottom, right, top;
        for (int y = 1; y < multiplier + 1; y++) {
            for (int x = 1; x < multiplier + 1; x++) {

                //x coordinates
                if (x == 1) {  //left border tile
                    left = 0.0F;
                    right = chunk;
                } else if (x == multiplier) { //right border tile
                    left = chunk * (multiplier - 1);
                    right = 1.0F;
                } else {    //tile not on left or right border...
                    left = chunk * (float) (x - 1);
                    right = chunk * (float) x;
                }

                //y coordinates
                if (y == 1) {  //bottom border tile
                    bottom = 0.0F;
                    top = chunk;
                } else if (y == multiplier) { //top border tile
                    bottom = chunk * (multiplier - 1);
                    top = 1.0F;
                } else {    //tile not on bottom or top border...
                    bottom = chunk * (float) (y - 1);
                    top = chunk * (float) y;
                }

                tileBorders[t] = new TileBorder(left, right, bottom, top);
                t++;
            }
        }
        return tileBorders;
    }

    /**
     * finds the index in tasks for given task value
     * @param taskId
     * @return
     */
    private int getTaskIndex(long taskId) {
        for (int i = 0; i < tasks.length; i++) {
            if (tasks[i].getId() == taskId) {
                return i;
            }
        }
        return -1;
    }

    /**
     * determines job status based
     * on values of counters: ABORTED, DONE, READY, requested, RUNNING and FAILED.
     */
    private void recalculateJobStatus() {
        readyTasks = 0;
        resetFlags();

        for (Task task : tasks) {
            switch (task.getStatus()) {

                case READY:
                    readyTasks++;
                    break;
                case RUNNING:
                    runningTasks++;
                    break;
                case DONE:
                    doneTasks++;
                    break;
                case FAILED:
                    failedTasks++;
                    break;
                default:
                    log.severe("unknown status type!");
            }
        }
        determineJobStatus();
    }

    /**
     * REMAINING_AND_STOPPED - remaining, stopped
     * REMAINING_AND_TASKS_RUNNING - remaining, tasks RUNNING
     * ALL_ASSIGNED_AND_RUNNING - all assigned, RUNNING
     * ALL_TASKS_FINISHED_OR_ABORTED - all tasks finished or ABORTED
     */
    private void determineJobStatus() {

        if (readyTasks > 0 && runningTasks < 1) {
            status = JobStatus.REMAINING_AND_STOPPED;
        } else if (readyTasks > 0 && runningTasks > 0) {
            status = JobStatus.REMAINING_AND_TASKS_RUNNING;
        } else if (readyTasks < 1 && runningTasks > 0) {
            status = JobStatus.ALL_ASSIGNED_AND_RUNNING;
        } else if (readyTasks < 1 && runningTasks < 1) {
            status = JobStatus.ALL_TASKS_FINISHED_OR_ABORTED;
        } else {
            log.severe("unknown job state!");
        }
    }

    public Integer getRenderingStep() {
        return renderingStep;
    }

    public void setRenderingStep(Integer renderingStep) {
        this.renderingStep = renderingStep;
    }

    public String getRunnableBenderFileName() {
        return runnableBenderFileName;
    }

    public boolean isEnabledAutoRunScripts() {
        return isEnabledAutoRunScripts;
    }
}
