package net.whn.loki.common;

public interface ICommon {

    enum LokiRole {
        GRUNT,
        GRUNT_COMMAND_LINE,
        MASTER,
        MASTER_GRUNT,
        ASK
    }

    /**
     *REMAINING_AND_STOPPED - remaining, stopped
     *REMAINING_AND_TASKS_RUNNING - remaining, tasks RUNNING
     *ALL_ASSIGNED_AND_RUNNING - all assigned, RUNNING
     *ALL_TASKS_FINISHED_OR_ABORTED - all tasks finished or ABORTED
     */
    enum JobStatus {
        REMAINING_AND_STOPPED,
        REMAINING_AND_TASKS_RUNNING,
        ALL_ASSIGNED_AND_RUNNING,
        ALL_TASKS_FINISHED_OR_ABORTED
    }

    /**
     * next task algorithm...
     * 
     */
    enum TaskStatus {
        READY,
        RUNNING,
        DONE,
        FAILED,
        LOCAL_ABORT,
        MASTER_ABORT,
        LOST_GRUNT
    }

    /**
     * expand this as needed
     */
    enum JobType { BLENDER }

    /**
     * expand as needed
     */
    enum MessageType {

        SHUTDOWN,
        FATAL_ERROR,
        START_QUEUE,
        ADD_JOB,
        VIEW_JOB,
        REMOVE_JOBS,
        ABORT_ALL,
        ADD_GRUNT,
        UPDATE_GRUNT,
        REMOVE_GRUNT,
        IDLE_GRUNT,
        VIEW_GRUNT,
        QUIT_GRUNT,
        LOST_BUSY_GRUNT,
        QUIT_ALL_GRUNTS,
        TASK_REPORT,
        RESET_FAILURES,
        FILE_REQUEST,
        FILE_RECEIVED
    }

    /**
     * grunt status as displayed in gruntList
     */
    enum GruntStatus { IDLE, RECEIVING, BUSY, SENDING, ERROR}

    /**
     * grunt update text status
     */
    enum GruntTxtStatus { 
        IDLE,
        BUSY,
        RECEIVING,
        PREP_CACHE,
        SEND,
        PENDING_SEND,
        ABORT,
        ERROR
    }

    /**
     * header type for network communication. this object is the lead object
     * sent on the stream and defines to the receiver what to do: carry out task
     * requesting a file, sending a file, etc. expand as needed
     */
    enum HeaderType {
        MACHINE_INFO,
        MACHINE_UPDATE,
        IDLE,
        FILE_REQUEST,
        FILE_REPLY, // mostly used for sending and receiving project file
        BUSY,
        TASK_ASSIGN,
        TASK_REPORT,
        TASK_ABORT,
        QUIT_AFTER_TASK,
        MASTER_SHUTDOWN,
        FILE_RECEIVED,
        DIFFERENT_LOKI_VERSION,
        TELL_LOKI_VERSION
    }
}
