package net.whn.loki.common;

import java.io.File;
import java.io.Serializable;

public class ProjectFileObject implements Serializable {

    /**
     * Could be either blend file or archive with many blend files.
     * For example like:
     * C:\Users\angel\Desktop\ManyBlendFiles.zip
     * or
     * C:\Users\angel\Desktop\BMW27GE.blend
     */
    private final File file;
    private long timeLastUsed;
    private volatile boolean inUse;
    private String name;

    public ProjectFileObject(File projectFile) {
       this.file = projectFile;
       this.name = projectFile.getName();
       timeLastUsed = System.currentTimeMillis();
       inUse = false;
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public long getSize() {
        return file.length();
    }

    public long getTimeLastUsed() {
        return timeLastUsed;
    }

    public void updateTime() {
        this.timeLastUsed = System.currentTimeMillis();
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
}
