package net.whn.loki.master;

import net.whn.loki.common.*;

import javax.swing.*;
import java.awt.*;

public class MasterEQCaller extends EQCaller {

    private static UpdateProgressBar updateProgressBar;
    private static boolean failureMsgOpen = false;

    /**
     * tell UI we're stopping the queue
     * @param mForm
     */
    public static void invokeStop(final MasterForm mForm) {
        EventQueue.invokeLater(() -> mForm.stopQueue());
    }

    /**
     * update the totalCore count on the UI
     * @param mForm
     * @param cores
     */
    public static void invokeUpdateCores(final MasterForm mForm, final int cores) {
        EventQueue.invokeLater(() -> mForm.updateCores(cores));
    }

    public static void invokeViewGruntDetails(final MasterForm masterForm, final GruntDetails details) {
        EventQueue.invokeLater(() -> masterForm.viewGruntDetails(details));
    }

    public static void invokeViewJobDetails(final MasterForm mForm, final Job job) {
        EventQueue.invokeLater(() -> mForm.viewJobDetails(job));
    }

    /**
     * update the task progress bar
     * @param mForm
     * @param update
     */
    public static void invokeUpdatePBar(final MasterForm mForm, final ProgressUpdate update) {

        updateProgressBar = new UpdateProgressBar(mForm, update);
        EventQueue.invokeLater(updateProgressBar);
    }

    public static void invokeTaskFailureNotification(final MasterForm mForm, final String failureStr) {
        EventQueue.invokeLater(() -> {
            Object[] options = {"OK", "Stop the Queue"};
            String prelude =
                    "One or more tasks in the job queue have failed.\n" +
                            "Below is output from the first failed task.\n" +
                            "You can view job\n details for more information.\n" +
                            "Viewing the log in <userdir>/.loki may also help.\n\n\"";
            if (!failureMsgOpen) {
                failureMsgOpen = true;
                int result = JOptionPane.showOptionDialog(
                        mForm,
                        prelude + failureStr + "\"",
                        "task failed",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[0]);

                if (result == 1) {
                    mForm.stopQueue();
                }
                failureMsgOpen = false;
            }
        });
    }

    private static class UpdateProgressBar implements Runnable {

        private final MasterForm masterForm;
        private final ProgressUpdate progressUpdate;

        UpdateProgressBar(MasterForm masterForm, ProgressUpdate progressUpdate) {
            this.masterForm = masterForm;
            this.progressUpdate = progressUpdate;
        }

        @Override
        public void run() {
            masterForm.updateProgressBar(progressUpdate);
        }
    }
}
