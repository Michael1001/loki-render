package net.whn.loki.common;

import java.io.Serializable;

public class TaskReport implements Serializable {

    Task task;

    public TaskReport(Task t) {
      task = t;
    }

    public Task getTask() {
        return task;
    }

    /**
     * should only be set by the broker when it receives the report
     * @param gID
     */
    public void setGruntID(long gID) {
        task.setGruntId(gID);
    }
}
