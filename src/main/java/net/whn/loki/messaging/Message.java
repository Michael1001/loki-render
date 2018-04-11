package net.whn.loki.messaging;

import net.whn.loki.common.ICommon;

public class Message implements ICommon {

    private final MessageType type;

    public Message(MessageType mType) {
        type = mType;
    }

    /**
     * fetches the message's type, as defined in the ICommon interface
     *
     * @return
     */
    public MessageType getType() {
        return type;
    }
}
