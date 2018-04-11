package net.whn.loki.error;

import java.io.IOException;

/**
 * Throw this exception when we've lost the connection to the grunt and
 * consequently we should attempt to gracefully close the socket, notify
 * and remove the broker.
 */
public class LostGruntException extends IOException {

    private final long gruntID;

    public LostGruntException(long gID) {
        gruntID = gID;
    }

    public long getGruntID() {
        return gruntID;
    }
}
