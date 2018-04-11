package net.whn.loki.messaging;

public class FileRequestMessage extends Message {

    private final String projectFileName;
    private final long gruntId;
    
    public FileRequestMessage(String projectFileName, long gruntId) {
        super(MessageType.FILE_REQUEST);
        this.projectFileName = projectFileName;
        this.gruntId = gruntId;
    }
    
    public String getProjectFileName() {
        return projectFileName;
    }

    public long getGruntId() {
        return gruntId;
    }

}
