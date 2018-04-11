package net.whn.loki.common;

import java.io.File;
import java.io.Serializable;

public class RenderedFileAttribute implements Serializable {

    private File file;
    private Long fileLength;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Long getFileLength() {
        return fileLength;
    }

    public void setFileLength(Long fileLength) {
        this.fileLength = fileLength;
    }
}
