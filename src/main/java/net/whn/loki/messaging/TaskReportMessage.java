package net.whn.loki.messaging;

import net.whn.loki.common.TaskReport;

public class TaskReportMessage extends Message {

    private final TaskReport report;

    public TaskReportMessage(TaskReport tr) {
        super(MessageType.TASK_REPORT);
        report = tr;
    }

    public TaskReport getReport() {
        return report;
    }
}
