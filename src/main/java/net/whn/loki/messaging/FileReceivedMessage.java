package net.whn.loki.messaging;

public class FileReceivedMessage extends Message {

    private long gruntID;

    public FileReceivedMessage(long gruntID) {
        super(MessageType.FILE_RECEIVED);
        this.gruntID = gruntID;
    }

    public long getGruntID() {
        return gruntID;
    }
}
