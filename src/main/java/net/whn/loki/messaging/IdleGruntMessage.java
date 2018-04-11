package net.whn.loki.messaging;

import net.whn.loki.common.ICommon;

public class IdleGruntMessage extends Message implements ICommon {

    private final long gruntID;

    public IdleGruntMessage(long gruntId) {
        super(MessageType.IDLE_GRUNT);
        gruntID = gruntId;
    }

    public long getGruntID() {
        return gruntID;
    }
}
