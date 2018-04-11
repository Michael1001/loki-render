package net.whn.loki.error;

public class FatalThrowable extends Throwable {

    public FatalThrowable(String msg) {
        super(msg);
        originalThrowable = this;
    }

    public FatalThrowable(Throwable e) {
        originalThrowable = e;
    }

    public Throwable getOriginalThrowable() {
        return originalThrowable;
    }

    private Throwable originalThrowable;
}
