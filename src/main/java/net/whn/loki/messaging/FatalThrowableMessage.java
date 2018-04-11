package net.whn.loki.messaging;

public class FatalThrowableMessage extends Message {

    Throwable throwable;

    public FatalThrowableMessage(MessageType m, Throwable e) {
        super(m);
        throwable = e;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
