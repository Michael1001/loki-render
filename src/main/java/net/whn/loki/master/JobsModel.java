package net.whn.loki.master;

import net.whn.loki.common.ICommon;
import net.whn.loki.common.ProgressUpdate;
import net.whn.loki.common.Task;
import net.whn.loki.common.TaskReport;
import net.whn.loki.io.MasterIOHelper;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds an array of job objects and serves as the model for the jobs queue table
 *
 * DEVNOTE: uses a synchronized list so all access is thread-safe, except
 * any calls that fetch an iterator(for each loop), these calls must be
 * synchronized on the list
 */
public class JobsModel extends AbstractTableModel implements ICommon, Serializable {

    private final List<Job> jobsList = Collections.synchronizedList(new ArrayList<Job>());
    private final String[] columnHeaders = getColumnHeaders();
    private final File lokiBaseFolder;

    /**
     * NOTE: make sure to update Job.getValue() if you change headers.
     * ...it's probably best to keep everything we want here, then just hide headers with the JTabel
     */
    public JobsModel(File lokiBaseFolder) {
        this.lokiBaseFolder = lokiBaseFolder;
    }

    /**
     * Initializes columnHeaders, from which is derived columnCount
     * @return
     */
    private String[] getColumnHeaders() {
        return new String[]{
                "name",
                "failed",
                "remain",
                "running",
                "done",
                "status"
        };
    }

    /**
     * Called by AWT EQ
     * @param c
     * @return the name of given column
     */
    @Override
    public String getColumnName(int c) {
        return columnHeaders[c];
    }

    /**
     * Called by AWT EQ
     * @return total number of columns
     */
    @Override
    public int getColumnCount() {
        return columnHeaders.length;
    }

    /**
     * Called by AWT EQ
     * 
     * @return current row count of the model.
     */
    @Override
    public int getRowCount() {
        return jobsList.size();
    }

    /**
     * Fetches the column value on specified row (job).
     * AWT
     * @param row
     * @param column
     * @return string value
     */
    @Override
    public Object getValueAt(int row, int column) {
        if (row < jobsList.size()) {
            return jobsList.get(row).getValue(column);
        } else {
            return "";
        }
    }

    public Job getJobDetails(int row) throws IOException, ClassNotFoundException {
        return MasterIOHelper.copyJob(jobsList.get(row));
    }

    public ProgressUpdate getProgressBarUpdate() {
        int totalTasks = 0;
        int remaining = 0;
        synchronized (jobsList) {
            for (Job job : jobsList) {
                totalTasks += job.getTotalTasks();
                remaining += job.getRemainingTasks();
            }
        }
        return new ProgressUpdate(totalTasks, totalTasks - remaining);
    }

    /**
     * called by AWT EQ -> areSelectedJobsRunning() in MasterR
     * used if user requests exit
     * @return
     */
    boolean areJobsRunning() {
        synchronized (jobsList) {
            for (Job j : jobsList) {
                if (j.getStatus() == JobStatus.REMAINING_AND_TASKS_RUNNING || j.getStatus() == JobStatus.ALL_ASSIGNED_AND_RUNNING) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean areSelectedJobsRunning(int[] selectedRows) {

        long[] selectedJobIDs = new long[selectedRows.length];
        synchronized (jobsList) {
            //populate our jobID array
            for (int i = 0; i < selectedRows.length; i++) {
                selectedJobIDs[i] = jobsList.get(selectedRows[i]).getJobId();
            }

            //now see if any of them are running
            for (int s = 0; s < selectedJobIDs.length; s++) {
                int jIndex = getJobIndex(selectedJobIDs[s]);
                if (jIndex != -1) {
                    Job j = jobsList.get(jIndex);
                    JobStatus iStatus = j.getStatus();
                    if (iStatus == JobStatus.REMAINING_AND_TASKS_RUNNING || iStatus == JobStatus.ALL_ASSIGNED_AND_RUNNING) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Called by master because we're going to remove the jobs which means we
     * first need to abort all running tasks in them.
     * NOTE! we're also setting taskstatus back to ready on the tasks!
     * @param selectedRows
     * @return
     */
    public ArrayList<Long> getGruntIDsForSelectedRunningJobs(int[] selectedRows) {

        long[] selectedJobIDs = new long[selectedRows.length];
        ArrayList<Long> gruntIDlist = new ArrayList<>();

        synchronized (jobsList) {
            //populate our jobID array
            for (int i = 0; i < selectedRows.length; i++) {
                selectedJobIDs[i] = jobsList.get(selectedRows[i]).getJobId();
            }

            //now see if any of them are running
            for (int s = 0; s < selectedJobIDs.length; s++) {
                int jIndex = getJobIndex(selectedJobIDs[s]);
                if (jIndex != -1) {
                    Job j = jobsList.get(jIndex);
                    JobStatus iStatus = j.getStatus();
                    if (iStatus == JobStatus.REMAINING_AND_TASKS_RUNNING || iStatus == JobStatus.ALL_ASSIGNED_AND_RUNNING) {
                        //found one, so add grunts to list
                        gruntIDlist = j.getGruntIDsForRunningTasks(gruntIDlist);
                    }
                }
            }
            return gruntIDlist;
        }
    }

    /**
     * called by master for abort all
     * NOTE! we're also setting taskstatus back to ready on the tasks!
     */
    public ArrayList<Long> getGruntIDsForAllRunningTasks() {
        ArrayList<Long> gruntIDlist = new ArrayList<>();
        synchronized (jobsList) {
            for (Job j : jobsList) {
                if (j.getStatus() == JobStatus.REMAINING_AND_TASKS_RUNNING || j.getStatus() == JobStatus.ALL_ASSIGNED_AND_RUNNING) {
                    gruntIDlist = j.getGruntIDsForRunningTasks(gruntIDlist);
                }
            }
        }
        fireTableDataChanged();
        return gruntIDlist;
    }

    long getJobID(int row) {
        return jobsList.get(row).getJobId();
    }

    /**
     * we're mutating the jobs, which AWT looks at
     * signals AWT
     *
     * @param jobId
     * @param taskID
     * @param status
     */
    public void setTaskStatus(long jobId, long taskID, TaskStatus status) {
        int jobIndex = getJobIndex(jobId);
        if (jobIndex != -1) {
            jobsList.get(jobIndex).setTaskStatus(taskID, status);
            fireTableRowsUpdated(jobIndex, jobIndex); //tell AWT.EventQueue
        }
    }

    public void resetFailures(int[] rows) {
        for(int i = 0; i<rows.length; i++) {
            jobsList.get(rows[i]).resetFailures();
        }
        fireTableDataChanged();
    }

    /**
     * This updates the job with all the info just returned from the grunt
     * -status
     * -gruntID
     * -output
     * and also calls compositer if all tiles are done
     * @param taskReport
     * @returns true if we need to composite tiles
     */
    public boolean setReturnTask(TaskReport taskReport) {

        Task task = taskReport.getTask();
        int jobIndex = getJobIndex(task.getJobId());
        if (jobIndex != -1) {
            Job job = jobsList.get(jobIndex);
            TaskStatus status = job.setReturnTask(taskReport);
            fireTableRowsUpdated(jobIndex, jobIndex); //tell AWT.EventQueue

            if (task.getStatus() == TaskStatus.DONE && task.isTile() && areFrameTilesDone(job, task.getFrame())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds given job to the end of jobsList, then tells AWT.EventQueue
     * called by addJob in master
     * junit: yes
     * @param job
     * @return true if job was successfully added, false if it wasn't
     */
    public void addJob(Job job) {
        jobsList.add(job);
        int newRow = jobsList.size() - 1;
        fireTableRowsInserted(newRow, newRow); //tell AWT.EventQueue
    }

    /**
     * Removes the jobs selected in the table, excepting running jobs
     * @param selectedRows
     */
    public void removeJobs(int[] selectedRows) {
        //we need to remove by jobID because indexes will change as we remove
        long[] selectedJobIDs = new long[selectedRows.length];

        //for loops all over in here; must synchronize!
        synchronized (jobsList) {
            //pull the jobIDs for the selected rows
            for (int i = 0; i < selectedRows.length; i++) {
                selectedJobIDs[i] = jobsList.get(selectedRows[i]).getJobId();
            }

            int currentIndex = -1;

            for (int s = 0; s < selectedJobIDs.length; s++) { //for each jobID...
                for (int i = 0; i < jobsList.size(); i++) {
                    if (selectedJobIDs[s] == jobsList.get(i).getJobId()) {
                        currentIndex = i;
                        break;
                    }
                }   //we now have the currentIndex of the job in jobsList that we'll remove

                jobsList.remove(currentIndex);
            }//end outer for
        }
        fireTableDataChanged(); //notify AWT
    }

    /**
     * Called by MasterR->AWT EQ
     * @return next available Task, null if none
     */
    public Task getNextTask() {
        synchronized (jobsList) {
            for (Job job : jobsList) {
                switch (job.getStatus()) {

                    case REMAINING_AND_STOPPED:
                    case REMAINING_AND_TASKS_RUNNING:
                        return job.getNextAvailableTask();
                }
            }
            return null;
        }
    }

    /**
     * Checks that all jobs are done (i.e. all tasks are complete
     * no tasks are running.
     * @return true if all tasks are done or aborted (ALL_TASKS_FINISHED_OR_ABORTED), false if otherwise
     */
    public boolean areAllJobsDone() {
        synchronized (jobsList) {
            for (Job job : jobsList) {
                if (job.getStatus() != JobStatus.ALL_TASKS_FINISHED_OR_ABORTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Called by master before adding a new job
     * @param nameToCheck
     * @return
     */
    public boolean isJobNameUnique(String nameToCheck) {

        //iterating on jobsList, so must synchronize!
        synchronized (jobsList) {
            for (Job j : jobsList) {
                if (nameToCheck.equals(j.getJobName())) {
                    return false;   //oops, name already exists
                }
            }
        }
        return true;
    }

    /**
     * Pass in the jID to get it's current index
     * DEVNOTE: this may change if any jobs are removed!
     * @param jID
     * @return the Job's current index
     */
    private int getJobIndex(long jID) {
        synchronized (jobsList) {
            for (int i = 0; i < jobsList.size(); i++) {
                if (jobsList.get(i).getJobId() == jID) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean areFrameTilesDone(Job job, int frame) {
        Task[] tasks = job.getTasks();
        for (int t = 0; t < tasks.length; t++) {
            if (tasks[t].getFrame() == frame) {
                if (tasks[t].getStatus() != TaskStatus.DONE) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isProjectFileOrphaned(String projectFileName) {
        boolean orphaned = true;
        synchronized (jobsList) {
            for (Job job : jobsList) {
                if (job.getProjectFileName().equals(projectFileName)) {
                    orphaned = false;
                    break;
                }
            }
        }
        return orphaned;
    }
}
