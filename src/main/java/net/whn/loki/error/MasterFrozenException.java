package net.whn.loki.error;

public class MasterFrozenException extends FatalThrowable {

    public MasterFrozenException(String msg) {
        super(msg);
    }
}
