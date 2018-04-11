package net.whn.loki.master;

import net.whn.loki.brokersModel.BrokersModel;
import net.whn.loki.common.*;
import net.whn.loki.common.configs.Config;
import net.whn.loki.error.ErrorHelper;
import net.whn.loki.error.MasterFrozenException;
import net.whn.loki.messaging.*;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * The main UI for the MasterR role of Loki. Jobs and grunts are managed
 */
public class MasterForm extends LokiForm implements ICommon {

    private static final Logger log = Logger.getLogger(MasterForm.class.toString());
    private final MasterR manager;
    private final JobsModel jobsModel;
    private final BrokersModel brokersModel;
    private PreferencesForm preferencesForm;
    private boolean queueRunning;

    private JToggleButton startButton;
    private JMenu fileMenu;
    private JTable gruntsTable;
    private JMenu helpMenu;
    private JLabel jobQueueLabel;
    private JLabel gruntsLabel;
    private JMenu gruntsMenu;
    private JMenuBar mainPageMenuBar;
    private JMenu jobsMenu;
    private JTable jobsTable;
    private JLabel coresLabel;
    private JMenuItem abortJobMenuItem;
    private JMenuItem aboutMenuItem;
    private JMenuItem newJobMenuItem;
    private JMenuItem preferencesMenuItem;
    private JMenuItem quitMenuItem;
    private JMenuItem removeJobMenuItem;
    private JMenuItem resetFailuresMenuItem;
    private JMenuItem viewGruntMenuItem;
    private JMenuItem viewJobMenuItem;
    private JPopupMenu gruntOptionsPopupMenu;
    private JPopupMenu jobOptionsPopupMenu;
    private JPopupMenu newJobPopupMenu;
    private JMenuItem abortAllMenuItem;
    private JMenuItem newJobContextMenuItem;
    private JMenuItem quitAllGruntsContextMenuItem;
    private JMenuItem quitGruntContextMenuItem;
    private JMenuItem removeJobContextMenuItem;
    private JMenuItem resetFailuresContextMenuItem;
    private JMenuItem viewGruntContextMenuItem;
    private JMenuItem viewJobContextMenuItem;
    private JProgressBar progressBarQueue;
    private JScrollPane gruntsScrollPane;
    private JScrollPane jobsScrollPane;

    public MasterForm(MasterR masterR) {

        manager = masterR;
        jobsModel = masterR.getJobsModel();
        brokersModel = masterR.getBrokersModel();

        initComponents();
    }

    private PreferencesForm buildPreferencesForm() {

        preferencesForm = new PreferencesForm(manager.getConfig());
        preferencesForm.setLocationRelativeTo(this);
        return preferencesForm;
    }

    /**
     * called by btnStartAction,
     * and by manager via EQCaller if all jobs are Status ALL_TASKS_FINISHED_OR_ABORTED (done and none
     * running)
     */
    public void stopQueue() {

        queueRunning = false;
        startButton.setText("Start");
        startButton.setSelected(queueRunning);
        manager.setQueueRunningFalse();
    }

    /**
     * called by manager via EventQueue when it has an update (range change, or
     * done has changed)
     */
    public void updateProgressBar(ProgressUpdate update) {

        String text = update.getDone() + "/" + update.getMax();
        progressBarQueue.setMaximum(update.getMax());
        progressBarQueue.setValue(update.getDone());
        progressBarQueue.setString(text);
    }

    public void updateCores(int cores) {
        coresLabel.setText("cores: " + Integer.toString(cores));
    }

    /**
     * called by manager
     *
     * @param details
     */
    public void viewGruntDetails(GruntDetails details) {

        GruntDetailsForm gruntDetailsForm = new GruntDetailsForm(details);
        gruntDetailsForm.setLocationRelativeTo(this);
        gruntDetailsForm.setVisible(true);
    }

    public void viewJobDetails(Job job) {

        JobDetailsForm jobDetailsForm = new JobDetailsForm(job);
        jobDetailsForm.setLocationRelativeTo(this);
        jobDetailsForm.setVisible(true);
    }

    /**
     * passes the newly created job to the MasterR
     * called by addJob in AddJobForm
     *
     * @param jobFormInput
     */
    void addJob(JobFormInput jobFormInput) {
        try {
            AddingJobForm addingJobForm = new AddingJobForm();
            addingJobForm.setLocationRelativeTo(this);
            addingJobForm.setVisible(true);
            manager.deliverMessage(new AddJobMessage(jobFormInput, addingJobForm));
        } catch (InterruptedException IntEx) {
            log.severe("interrupted exception: " + IntEx.toString());
        } catch (MasterFrozenException mfe) {
            ErrorHelper.outputToLogMsgAndKill(this, false, log, "fatal error. click ok to exit.", mfe.getCause());
            System.exit(-1);
        }
    }

    private void initComponents() {

        buildMainGroupLayout();

        setJMenuBar(buildMainPageMenuBar());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Loki Render - master");
        setName("managerForm"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                shutdown();
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    private void buildCoresLabel() {

        coresLabel = new JLabel("cores: 0");
        coresLabel.setToolTipText("displays the total number of CPU cores currently in the farm");
    }

    private void buildProgressBar() {

        progressBarQueue = new JProgressBar();
        progressBarQueue.setToolTipText("displays the progress of finished tasks in the job queue");
        progressBarQueue.setStringPainted(true);
    }

    private JScrollPane buildGruntsScrollPane() {

        gruntsScrollPane = new JScrollPane();
        gruntsScrollPane.setViewportView(buildGruntsTable());
        return gruntsScrollPane;
    }

    private JTable buildGruntsTable() {

        gruntsTable = new JTable(brokersModel);
        gruntsTable.setToolTipText("right-click for a context-sensitive popup menu");
        gruntsTable.setComponentPopupMenu(buildGruntOptionsPopupMenu());
        gruntsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return gruntsTable;
    }

    private JScrollPane buildJobScrollPane() {

        jobsScrollPane = new JScrollPane();
        jobsScrollPane.setToolTipText("right-click for a context-sensitive popup menu");
        jobsScrollPane.setViewportView(buildJobsTable());
        jobsScrollPane.setComponentPopupMenu(buildNewJobPopupMenu());
        return jobsScrollPane;
    }

    private JTable buildJobsTable() {

        jobsTable = new JTable(jobsModel);
        jobsTable.setToolTipText("right-click for a context-sensitive popup menu");
        jobsTable.setComponentPopupMenu(buildJobOptionsPopupMenu());
        return jobsTable;
    }

    private JPopupMenu buildNewJobPopupMenu() {

        newJobPopupMenu = new JPopupMenu();
        newJobPopupMenu.add(buildNewJobContextMenuItem());
        return newJobPopupMenu;
    }

    private JMenuItem buildNewJobContextMenuItem() {

        newJobContextMenuItem = new JMenuItem("New Job");
        newJobContextMenuItem.addActionListener(event -> showAddJobForm());
        return newJobContextMenuItem;
    }

    private JPopupMenu buildGruntOptionsPopupMenu() {

        gruntOptionsPopupMenu = new JPopupMenu();
        gruntOptionsPopupMenu.add(buildViewGruntContextMenuItem());
        gruntOptionsPopupMenu.add(buildQuitGruntContextMenuItem());
        gruntOptionsPopupMenu.add(buildQuitAllGruntsContextMenuItem());
        return gruntOptionsPopupMenu;
    }

    private JMenuItem buildQuitAllGruntsContextMenuItem() {

        quitAllGruntsContextMenuItem = new JMenuItem("Quit all grunts after tasks (quit now if idle)");
        quitAllGruntsContextMenuItem.addActionListener(event -> quitAllGrunts());
        return quitAllGruntsContextMenuItem;
    }

    private JMenuItem buildQuitGruntContextMenuItem() {

        quitGruntContextMenuItem = new JMenuItem("Quit after task (quit now if idle)");
        quitGruntContextMenuItem.addActionListener(event -> quitGrunt());
        return quitGruntContextMenuItem;
    }

    private JMenuItem buildViewGruntContextMenuItem() {

        viewGruntContextMenuItem = new JMenuItem("View details");
        viewGruntContextMenuItem.addActionListener(event -> viewGrunt());
        return viewGruntContextMenuItem;
    }

    private JPopupMenu buildJobOptionsPopupMenu() {

        jobOptionsPopupMenu = new JPopupMenu();
        jobOptionsPopupMenu.add(buildViewJobContextMenuItem());
        jobOptionsPopupMenu.add(buildRemoveJobContextMenuItem());
        jobOptionsPopupMenu.add(buildResetFailuresContextMenuItem());
        jobOptionsPopupMenu.add(buildAbortAllMenuItem());
        return jobOptionsPopupMenu;
    }

    private JMenuItem buildAbortAllMenuItem() {

        abortAllMenuItem = new JMenuItem("Abort all and stop queue");
        abortAllMenuItem.addActionListener(event -> abortAllJobs());
        return abortAllMenuItem;
    }

    private JMenuItem buildResetFailuresContextMenuItem() {

        resetFailuresContextMenuItem = new JMenuItem("Reset failed tasks");
        resetFailuresContextMenuItem.addActionListener(event -> resetFailures());
        return resetFailuresContextMenuItem;
    }

    private JMenuItem buildRemoveJobContextMenuItem() {

        removeJobContextMenuItem = new JMenuItem("Remove");
        removeJobContextMenuItem.addActionListener(event -> removeJob());
        return removeJobContextMenuItem;
    }

    private JMenuItem buildViewJobContextMenuItem() {

        viewJobContextMenuItem = new JMenuItem("View details");
        viewJobContextMenuItem.addActionListener(event -> viewJob());
        return viewJobContextMenuItem;
    }

    private void buildStartButton() {
        startButton = new JToggleButton("Start");
        startButton.setToolTipText("start and stop the job queue");
        startButton.addActionListener(event -> startQueue());
    }

    private void startQueue() {
        if (!queueRunning) { //start the queue
            queueRunning = true;
            try {
                manager.deliverMessage(new Message(MessageType.START_QUEUE));
                startButton.setText("Stop");
            } catch (InterruptedException ex) {
                ErrorHelper.outputToLogMsgAndKill(this, false, log, "fatal error. click ok to exit.", ex);
            } catch (MasterFrozenException mfe) {
                ErrorHelper.outputToLogMsgAndKill(this, false, log, "fatal error. click ok to exit.", mfe.getCause());
                System.exit(-1);
            }
        } else {  //stop the queue
            stopQueue();
        }
        startButton.setSelected(queueRunning);
    }

    private JMenuBar buildMainPageMenuBar() {

        mainPageMenuBar = new JMenuBar();
        mainPageMenuBar.add(buildFileMenu());
        mainPageMenuBar.add(buildJobsMenu());
        mainPageMenuBar.add(buildGruntsMenu());
        mainPageMenuBar.add(buildHelpMenu());
        return mainPageMenuBar;
    }

    private JMenu buildHelpMenu() {

        helpMenu = new JMenu("Help");
        helpMenu.add(buildAboutMenuItem());
        return helpMenu;
    }

    private JMenuItem buildAboutMenuItem() {

        aboutMenuItem = new JMenuItem();
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(event -> showAboutMenuItem());
        return aboutMenuItem;
    }

    private JMenu buildGruntsMenu() {

        gruntsMenu = new JMenu("Grunts");
        gruntsMenu.add(buildViewGruntMenuItem());
        return gruntsMenu;
    }

    private JMenuItem buildViewGruntMenuItem() {

        viewGruntMenuItem = new JMenuItem("View Details");
        viewGruntMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        viewGruntMenuItem.addActionListener(event -> viewGrunt());
        return viewGruntMenuItem;
    }

    private JMenu buildJobsMenu() {

        jobsMenu = new JMenu("Jobs");
        jobsMenu.add(buildNewJobMenuItem());
        jobsMenu.add(buildViewJobMenuItem());
        jobsMenu.add(buildRemoveJobMenuItem());
        jobsMenu.add(buildResetFailuresMenuItem());
        jobsMenu.add(buildAbortJobMenuItem());
        return jobsMenu;
    }

    private JMenuItem buildAbortJobMenuItem() {

        abortJobMenuItem = new JMenuItem();
        abortJobMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        abortJobMenuItem.setText("Abort all and stop queue");
        abortJobMenuItem.addActionListener(event -> abortAllJobs());
        return abortJobMenuItem;
    }

    private JMenuItem buildResetFailuresMenuItem() {

        resetFailuresMenuItem = new JMenuItem();
        resetFailuresMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        resetFailuresMenuItem.setText("Reset failed tasks");
        resetFailuresMenuItem.addActionListener(event -> resetFailures());
        return resetFailuresMenuItem;
    }

    private JMenuItem buildRemoveJobMenuItem() {

        removeJobMenuItem = new JMenuItem();
        removeJobMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        removeJobMenuItem.setText("Remove");
        removeJobMenuItem.addActionListener(event -> removeJob());
        return removeJobMenuItem;
    }

    private JMenuItem buildViewJobMenuItem() {

        viewJobMenuItem = new JMenuItem("View details");
        viewJobMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, java.awt.event.InputEvent.CTRL_MASK));
        viewJobMenuItem.addActionListener(event -> viewJob());
        return viewJobMenuItem;
    }

    private JMenuItem buildNewJobMenuItem() {

        newJobMenuItem = new JMenuItem("New");
        newJobMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newJobMenuItem.addActionListener(event -> showAddJobForm());
        return newJobMenuItem;
    }

    private JMenu buildFileMenu() {

        fileMenu = new JMenu("File");
        fileMenu.add(buildPreferencesMenuItem());
        fileMenu.add(buildQuitMenuItem());
        return fileMenu;
    }

    private JMenuItem buildQuitMenuItem() {

        quitMenuItem = new JMenuItem();
        quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        quitMenuItem.setText("Quit");
        quitMenuItem.addActionListener(event -> shutdown());
        return quitMenuItem;
    }

    private JMenuItem buildPreferencesMenuItem() {

        buildPreferencesForm();
        preferencesMenuItem = new JMenuItem("Preferences   ");
        preferencesMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        preferencesMenuItem.addActionListener(event -> showPreferencesForm());
        return preferencesMenuItem;
    }

    private void buildMainGroupLayout() {

        jobQueueLabel = new JLabel("Job Queue");
        gruntsLabel = new JLabel("Grunts");

        buildProgressBar();
        buildCoresLabel();
        buildStartButton();
        buildJobScrollPane();
        buildGruntsScrollPane();

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                        .addComponent(jobQueueLabel)
                                                        .addComponent(jobsScrollPane, GroupLayout.DEFAULT_SIZE, 417, Short.MAX_VALUE)
                                                )
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                        .addComponent(gruntsLabel)
                                                        .addComponent(gruntsScrollPane, GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE)
                                                )
                                                .addContainerGap()
                                        )
                                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addComponent(coresLabel)
                                                .addGap(12, 12, 12)
                                        )
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(startButton, GroupLayout.PREFERRED_SIZE, 151, GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(progressBarQueue, GroupLayout.DEFAULT_SIZE, 671, Short.MAX_VALUE)
                                                .addContainerGap()
                                        )
                                )
                        )
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(gruntsLabel)
                                        .addComponent(jobQueueLabel)
                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(gruntsScrollPane, GroupLayout.DEFAULT_SIZE, 294, Short.MAX_VALUE)
                                        .addComponent(jobsScrollPane, GroupLayout.DEFAULT_SIZE, 294, Short.MAX_VALUE)
                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(coresLabel)
                                .addGap(7, 7, 7)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(startButton, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(progressBarQueue, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                )
                                .addContainerGap()
                        )
        );
    }

    private void showPreferencesForm() {

        preferencesForm.updateCacheValues();
        preferencesForm.setVisible(true);
    }

    private void showAboutMenuItem() {
        AboutForm aboutForm = new AboutForm();
        aboutForm.setLocationRelativeTo(this);
        aboutForm.setVisible(true);
    }

    /**
     * opens a new AddJobForm
     */
    private void showAddJobForm() {
        AddJobForm newJobForm = new AddJobForm(this, manager.getJobsModel());
        newJobForm.setLocationRelativeTo(this);
        newJobForm.setVisible(true);
    }

    private void abortAllJobs() {
        if (manager.areJobsRunning()) {
            int result = JOptionPane.showConfirmDialog(this,
                    "All running tasks will be aborted. Continue?",
                    "Abort all jobs and stop queue?",
                    JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == 0) {
                sendMessageToManager(new Message(MessageType.ABORT_ALL));
            }
        } else {
            sendMessageToManager(new Message(MessageType.ABORT_ALL));
        }

    }

    private void quitGrunt() {
        int selectedGrunt = gruntsTable.getSelectedRow();
        if (selectedGrunt != -1) {
            sendMessageToManager(new SelectedGruntMessage(MessageType.QUIT_GRUNT, selectedGrunt));
        } else {
            JOptionPane.showMessageDialog(this, "Please select a grunt first.");
        }
    }

    private void quitAllGrunts() {
        sendMessageToManager(new Message(MessageType.QUIT_ALL_GRUNTS));
    }

    private void viewGrunt() {
        int selectedGrunt = gruntsTable.getSelectedRow();
        if (selectedGrunt != -1) {
            sendMessageToManager(new SelectedGruntMessage(MessageType.VIEW_GRUNT, selectedGrunt));
        } else {
            JOptionPane.showMessageDialog(this, "Please select a grunt first.");
        }
    }

    private void viewJob() {
        int selectedJob = jobsTable.getSelectedRow();
        if (selectedJob != -1) {
            sendMessageToManager(new SelectedGruntMessage(MessageType.VIEW_JOB, selectedJob));
        } else {
            JOptionPane.showMessageDialog(this, "Please select a job, then select 'View Job Details'.");
        }
    }

    private void resetFailures() {
        int[] rows = jobsTable.getSelectedRows();

        if (rows.length > 0) {
            sendMessageToManager(new ResetFailuresMessage(MessageType.RESET_FAILURES, rows));
        } else {
            JOptionPane.showMessageDialog(this, "Please select one or more jobs first.");
        }
    }

    private void removeJob() {
        int[] rows = jobsTable.getSelectedRows();

        if (rows.length > 0) {
            if (manager.areJobsRunning(rows)) {
                int result = JOptionPane.showConfirmDialog(this,
                        "One or more selected jobs have running tasks.\n" +
                                "The tasks will be aborted and the jobs will be\n" +
                                "removed from the queue. Continue?",
                        "Abort selected jobs and remove?",
                        JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == 0) {
                    sendMessageToManager(new RemoveJobsMessage(MessageType.REMOVE_JOBS, rows));
                }
            } else {
                sendMessageToManager(new RemoveJobsMessage(MessageType.REMOVE_JOBS, rows));
            }

        } else {
            JOptionPane.showMessageDialog(this, "Please select one or more jobs, then select 'Remove'.");
        }
    }

    /**
     * sends shutdown message to the MasterR, then closes the application
     */
    private void shutdown() {
        boolean exit = false;
        if (!manager.areJobsRunning()) {
            exit = true;
        } else {
            int result = JOptionPane.showConfirmDialog(this,
                    "If you quit the master now, grunts will continue\n" +
                            "running tasks, then wait to send their output\n" +
                            "files to the master when it starts again. However,\n" +
                            "the local grunt's task progress (if a local grunt\n" +
                            "is running) will be lost. Are you sure you want to quit?",
                    "Quit the master?",
                    JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == 0) {
                exit = true;
            }
        }

        if (exit) {
            preferencesForm.dispose();
            dispose();
            sendMessageToManager(new Message(MessageType.SHUTDOWN));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                System.exit(0);
            }
        }
    }

    /**
     * just a little helper method so we don't need try/catch everywhere
     *
     * @param message
     */
    private void sendMessageToManager(Message message) {
        try {
            manager.deliverMessage(message);
        } catch (InterruptedException ex) {
            ErrorHelper.outputToLogMsgAndKill(this, false, log, "fatal error. click ok to exit.", ex);
            System.exit(-1);
        } catch (MasterFrozenException mfe) {
            ErrorHelper.outputToLogMsgAndKill(this, false, log, "fatal error. click ok to exit.", mfe.getCause());
            System.exit(-1);
        }
    }

    public Config getCfg() {
        return manager.getConfig();
    }
}
