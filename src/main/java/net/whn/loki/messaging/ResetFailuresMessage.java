package net.whn.loki.messaging;

public class ResetFailuresMessage extends Message {

    int[] rows;

    public ResetFailuresMessage(MessageType t, int[] r) {
        super(t);
        rows = r;
    }

    public int[] getRows() {
        return rows;
    }
}
