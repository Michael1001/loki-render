package net.whn.loki.network;

import net.whn.loki.common.*;

import java.io.Serializable;

public class Header implements Serializable, ICommon {

    private final HeaderType headerType;
    private final TaskReport report;
    private final Machine machine;
    private final MachineUpdate machineUpdate;
    private final Task task;
    private final String projectFileName;
    private final long projectFileSize;
    private String lokiVersion;

    // used as well in the FILES_RECIEVED, among others
    public Header(HeaderType headerType) {
        this.headerType = headerType;
        report = null;
        machine = null;
        machineUpdate = null;
        task = null;
        projectFileName = null;
        projectFileSize = -1;
    }

    //constructor for MACHINE_INFO
    public Header(HeaderType headerType, Machine machine) {
        this.headerType = headerType;
        report = null;
        this.machine = machine;
        machineUpdate = null;
        task = null;
        projectFileName = null;
        projectFileSize = -1;
    }

    //constructor for MACHINE_UPDATE
    public Header(HeaderType headerType, MachineUpdate machineUpdate) {
        this.headerType = headerType;
        report = null;
        machine = null;
        this.machineUpdate = machineUpdate;
        task = null;
        projectFileName = null;
        projectFileSize = -1;
    }

    //constructor for TASK_SEND
    public Header(HeaderType headerType, Task task) {
        this.headerType = headerType;
        report = null;
        machine = null;
        machineUpdate = null;
        this.task = task;
        projectFileName = null;
        projectFileSize = -1;
    }

    //constructor for FILE_REQUEST
    public Header(HeaderType headerType, String projectFileName) {
        this.headerType = headerType;
        report = null;
        machine = null;
        machineUpdate = null;
        task = null;
        this.projectFileName = projectFileName;
        projectFileSize = -1;
    }

    //constructor for FILE_REPLY
    public Header(HeaderType headerType, String projectFileName, long projectFileSize) {
        this.headerType = headerType;
        report = null;
        machine = null;
        machineUpdate = null;
        task = null;
        this.projectFileName = projectFileName;
        this.projectFileSize = projectFileSize;
    }

    //constructor for TASK_REPORT
    public Header(HeaderType headerType, TaskReport taskReport) {
        this.headerType = headerType;
        report = taskReport;
        machine = null;
        machineUpdate = null;
        task = null;
        projectFileName = null;
        projectFileSize = -1;
    }

    //constructor for DIFFERENT_LOKI_VERSION and TELL_LOKI_VERSION
    public Header(String lokiVersion, HeaderType headerType) {

        this.lokiVersion = lokiVersion;
        this.headerType = headerType;
        report = null;
        machine = null;
        machineUpdate = null;
        task = null;
        projectFileName = null;
        projectFileSize = -1;
    }

    public HeaderType getHeaderType() {
        return headerType;
    }

    public Machine getMachine() {
        return machine;
    }

    public MachineUpdate getMachineUpdate() {
        return machineUpdate;
    }

    public Task getTask() {
        return task;
    }

    public TaskReport getTaskReport() {
        return report;
    }

    public String getProjectFileName() {
        return projectFileName;
    }

    public long getProjectFileSize() {
        return projectFileSize;
    }

    public String getLokiVersion() {
        return lokiVersion;
    }
}
