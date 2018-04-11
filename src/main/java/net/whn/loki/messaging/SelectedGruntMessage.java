package net.whn.loki.messaging;

public class SelectedGruntMessage extends Message {

    private final MessageType messageType;
    private final int row;

    public SelectedGruntMessage(MessageType messageType, int row) {
        super(messageType);
        this.messageType = messageType;
        this.row = row;
    }

    public int getRow() {
        return row;
    }
}
