package net.whn.loki.messaging;

import net.whn.loki.common.Task;

public class RemoveGruntMessage extends Message {

    final private long gruntID;
    final private GruntStatus status;
    final private Task task;

    public RemoveGruntMessage(long gruntId) {
        this(gruntId, GruntStatus.IDLE, null);
    }

    public RemoveGruntMessage(long gruntId, GruntStatus gruntStatus, Task task) {
        super(MessageType.REMOVE_GRUNT);
        gruntID = gruntId;
        status = gruntStatus;
        this.task = task;
    }

    public long getGruntID() {
        return gruntID;
    }

    public GruntStatus getGruntStatus() {
        return status;
    }

    public Task getGruntTask() {
        return task;
    }
}
