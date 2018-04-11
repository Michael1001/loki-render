package net.whn.loki.grunt;

import net.whn.loki.common.ICommon;

public class GruntStatusText implements ICommon {

    private final GruntTxtStatus statusText;
    private long fileSize;

    GruntStatusText(GruntTxtStatus sText) {
        statusText = sText;
        fileSize = 0;
    }

    GruntStatusText(GruntTxtStatus sText, long fSize) {
        this(sText);
        fileSize = fSize;
    }

    GruntTxtStatus getStatus() {
        return statusText;
    }

    long getFileSize() {
        return fileSize;
    }
}
