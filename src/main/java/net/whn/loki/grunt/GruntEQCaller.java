package net.whn.loki.grunt;

import net.whn.loki.common.*;

import java.awt.*;

public class GruntEQCaller extends EQCaller {

    private static UpdateProgressBar updateProgressBar;
    
    /**
     * updates the connection label on gruntForm
     * @param gruntForm
     * @param connectionLabelMessage
     */
    public static void invokeUpdateConnectionLbl(final GruntForm gruntForm, final String connectionLabelMessage) {

        EventQueue.invokeLater(() -> gruntForm.setConnectionLabelMessage(connectionLabelMessage));
    }

    /**
     * Updates the status label on gruntForm
     * @param gruntForm
     * @param gruntStatusText
     */
    public static void invokeUpdateStatus(final GruntForm gruntForm, final GruntStatusText gruntStatusText) {

        EventQueue.invokeLater(() -> gruntForm.setStatus(gruntStatusText));
    }

    /**
     * Update the task progress bar
     * @param gruntForm
     * @param progressUpdate
     */
    public static void invokeGruntUpdatePBar(final GruntForm gruntForm, final ProgressUpdate progressUpdate) {

        updateProgressBar = new UpdateProgressBar(gruntForm, progressUpdate);
        EventQueue.invokeLater(updateProgressBar);
    }

    private static class UpdateProgressBar implements Runnable {

        private final GruntForm gruntForm;
        private final ProgressUpdate progressUpdate;

        UpdateProgressBar(GruntForm gruntForm, ProgressUpdate progressUpdate) {
           this.gruntForm = gruntForm;
           this.progressUpdate = progressUpdate;
        }

        @Override
        public void run() {
            gruntForm.updateProgressBar(progressUpdate);
        }
    }
}
