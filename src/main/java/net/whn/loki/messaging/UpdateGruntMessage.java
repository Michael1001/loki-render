package net.whn.loki.messaging;

import net.whn.loki.common.*;

public class UpdateGruntMessage extends Message implements ICommon {

    private final long gruntID;
    private final boolean firstMachineReply;

    public UpdateGruntMessage(long gruntId, boolean first) {
        super(MessageType.UPDATE_GRUNT);
        gruntID = gruntId;
        firstMachineReply = first;
    }

    public long getGruntID() {
        return gruntID;
    }

    public boolean isFirstMachineReply() {
        return firstMachineReply;
    }
}
