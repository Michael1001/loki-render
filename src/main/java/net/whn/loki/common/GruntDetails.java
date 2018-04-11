package net.whn.loki.common;

public class GruntDetails {

    private final String hostname;
    private final String osName;
    private final String osVersion;
    private final String osArchitecture;
    private final int processors;
    private final long totalMemory;
    private final long totalSwap;
    private final String userName;
    private final String userHome;
    private final String currentWorkingFolder;
    private final String gruntStatus;
    private final String gruntErrorDetails;

    public GruntDetails(
            String hostname,
            String osName,
            String osVersion,
            String osArchitecture,
            int cores,
            long totalMemory,
            long totalSwap,
            String userName,
            String userHome,
            String currentWorkingFolder,
            String gruntStatus,
            String gruntErrorDetails) {
        
        this.hostname = hostname;
        this.osName = osName;
        this.osVersion = osVersion;
        this.osArchitecture = osArchitecture;
        processors = cores;
        this.totalMemory = totalMemory;
        this.totalSwap = totalSwap;
        this.userName = userName;
        this.userHome = userHome;
        this.currentWorkingFolder = currentWorkingFolder;
        this.gruntStatus = gruntStatus;
        this.gruntErrorDetails = gruntErrorDetails;
    }

    public String getCurrentWorkingFolder() {
        return currentWorkingFolder;
    }

    public String getHostname() {
        return hostname;
    }

    public String getOsArchitecture() {
        return osArchitecture;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public int getProcessors() {
        return processors;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public long getTotalSwap() {
        return totalSwap;
    }

    public String getUserHome() {
        return userHome;
    }

    public String getUserName() {
        return userName;
    }

    public String getGruntStatus() {
        return gruntStatus;
    }

    public String getGruntErrorDetails() {
        return gruntErrorDetails;
    }
}
