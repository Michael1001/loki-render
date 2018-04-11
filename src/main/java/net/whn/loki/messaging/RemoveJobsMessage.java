package net.whn.loki.messaging;

public class RemoveJobsMessage extends Message {

    private final int[] rowsToRemove;

    public RemoveJobsMessage(MessageType t, int[] rows) {
        super(t);
        rowsToRemove = rows;
    }

    public int[] getRowsToRemove() {
        return rowsToRemove;
    }
}
