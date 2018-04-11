package net.whn.loki.messaging;

import net.whn.loki.common.LokiForm;
import net.whn.loki.master.*;

public class AddJobMessage extends Message {

    private JobFormInput jobFormInput;
    private LokiForm addingJobForm;
    
    public AddJobMessage(JobFormInput jobFormInput, LokiForm addingJobForm) {
        super(MessageType.ADD_JOB);
        this.jobFormInput = jobFormInput;
        this.addingJobForm = addingJobForm;
    }

    public JobFormInput getJobFormInput() {
        return jobFormInput;
    }

    public void disposeAddingJobForm() {
        addingJobForm.dispose();
    }
}
