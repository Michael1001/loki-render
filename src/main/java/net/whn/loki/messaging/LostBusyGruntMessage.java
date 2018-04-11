package net.whn.loki.messaging;

import net.whn.loki.common.Task;

public class LostBusyGruntMessage extends Message {

    final private long gruntID;
    final private Task task;

    public LostBusyGruntMessage(long gID, Task t) {
        super(MessageType.LOST_BUSY_GRUNT);
        gruntID = gID;
        task = t;
    }

    public Task getGruntTask() {
        return task;
    }
}
