package net.whn.loki.grunt;

import net.whn.loki.common.*;
import net.whn.loki.common.ICommon.TaskStatus;
import net.whn.loki.io.GruntIOHelper;

import javax.swing.*;
import java.util.logging.Logger;

public class GruntForm extends LokiForm {

    private static final Logger log = Logger.getLogger(GruntForm.class.toString());
    private JMenuBar jMenuBar;
    private JLabel connectionLabelMessage;
    private JLabel statusLabelMessage;
    private JMenu taskMenu;
    private JProgressBar progressBar;
    private final GruntR grunt;
    private final PreferencesForm preferencesForm;

    public GruntForm(GruntR g) {
        initComponents();
        grunt = g;
        preferencesForm = new PreferencesForm(grunt.getCfg());
        preferencesForm.setLocationRelativeTo(this);

        //log.setLevel(Level.FINE);
    }

    public void setStatus(GruntStatusText gruntStatusText) {

        switch (gruntStatusText.getStatus()) {

            case IDLE:
                statusLabelMessage.setText("idle");
                progressBar.setValue(0);
                progressBar.setIndeterminate(false);
                break;
            case BUSY:
                statusLabelMessage.setText("busy");
                progressBar.setIndeterminate(true);
                break;
            case RECEIVING:
                String fetchInfo = "fetching file " + GruntIOHelper.generateHumanReadableFileSize(gruntStatusText.getFileSize());
                statusLabelMessage.setText(fetchInfo);
                progressBar.setIndeterminate(false);
                log.fine(fetchInfo);
                break;
            case PREP_CACHE:
                statusLabelMessage.setText("preparing blendcache");
                progressBar.setIndeterminate(true);
                break;
            case SEND:
                String sendInfo = "sending output file " + GruntIOHelper.generateHumanReadableFileSize(gruntStatusText.getFileSize());
                statusLabelMessage.setText(sendInfo);
                progressBar.setIndeterminate(false);
                log.fine(sendInfo);
                break;
            case PENDING_SEND:
                statusLabelMessage.setText("waiting to send output file");
                progressBar.setIndeterminate(false);
                break;
            case ABORT:
                statusLabelMessage.setText("task aborted");
                progressBar.setValue(0);
                progressBar.setIndeterminate(false);
                break;
            case ERROR:
                statusLabelMessage.setText("error");
                progressBar.setValue(0);
                progressBar.setIndeterminate(false);
                break;
            default:
                log.warning("unknown setStatus value: " + gruntStatusText);
        }
    }

    /**
     * called by grunt via EventQueue when it has an update
     */
    public void updateProgressBar(ProgressUpdate update) {
        //progressBar.setIndeterminate(false);
        progressBar.setMaximum(update.getMax());
        progressBar.setValue(update.getDone());
    }

    public void exitNoQuery() {
        grunt.signalShutdown();
        preferencesForm.dispose();
        dispose();
    }

    private JProgressBar buildProgressBar() {
        progressBar = new JProgressBar();
        progressBar.setToolTipText("displays activity while a task is running, or progress bar during file transfer");
        return progressBar;
    }

    private JPanel buildProgressBarPanel() {
        JPanel progressBarPanel = new JPanel();
        buildProgressBarLayout(progressBarPanel, buildProgressBar());
        return progressBarPanel;
    }

    private JPanel buildConnectionStatusPanel() {

        JPanel connectionStatusPanel = new JPanel();
        buildConnectionLabelMessage();
        buildConnectionStatusLayout(connectionStatusPanel, new JLabel("Connection:"), new JLabel("My status:"));
        return connectionStatusPanel;
    }

    private JPanel buildGapPanel() {
        JPanel gapPanel = new JPanel();
        buildGapGroupLayout(gapPanel);
        return gapPanel;
    }

    private void buildConnectionLabelMessage() {
        connectionLabelMessage = new JLabel();
        connectionLabelMessage.setText("attempting to connect with master...");
        connectionLabelMessage.setToolTipText("current connection status with the master");
    }

    private void initComponents() {

        addWindowSettings();




        statusLabelMessage = new JLabel();




        jMenuBar = new JMenuBar();

        taskMenu = new JMenu();

        statusLabelMessage.setText("idle");
        statusLabelMessage.setToolTipText("this grunt's current status");


        jMenuBar.add(buildFileMenu());

        taskMenu.setText("Task");
        taskMenu.add(buildAbortMenuItem());

        jMenuBar.add(taskMenu);
        jMenuBar.add(buildHelpMenu());

        setJMenuBar(jMenuBar);
        buildMainLayout(buildProgressBarPanel(), buildGapPanel(), buildConnectionStatusPanel());

        pack();
    }

    private JMenuItem buildPreferencesMenuItem() {

        JMenuItem preferencesMenuItem = new JMenuItem();
        preferencesMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        preferencesMenuItem.setText("Preferences");
        preferencesMenuItem.addActionListener(event -> {
            preferencesForm.updateCacheValues();
            preferencesForm.setVisible(true);
        });
        return preferencesMenuItem;
    }

    private JMenu buildFileMenu() {

        JMenu fileMenu = new JMenu();
        fileMenu.setText("File");
        fileMenu.add(buildViewLogMenuItem());
        fileMenu.add(buildPreferencesMenuItem());
        fileMenu.add(buildQuitMenuItem());
        return fileMenu;
    }

    private JMenuItem buildViewLogMenuItem() {

        JMenuItem viewLogMenuItem = new JMenuItem();
        viewLogMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        viewLogMenuItem.setText("View Log");
        viewLogMenuItem.setEnabled(false);
        return viewLogMenuItem;
    }

    private JMenuItem buildQuitMenuItem() {

        JMenuItem quitMenuItem = new JMenuItem();
        quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        quitMenuItem.setText("Quit");
        quitMenuItem.addActionListener(event -> exitQuery());
        return quitMenuItem;
    }

    private JMenuItem buildAbortMenuItem() {

        JMenuItem abortMenuItem = new JMenuItem();
        abortMenuItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        abortMenuItem.setText("Abort and quit");
        abortMenuItem.addActionListener(event -> abort());
        return abortMenuItem;
    }

    private void buildMainLayout(JPanel progressBarPanel, JPanel gapPanel, JPanel connectionStatusPanel) {

        GroupLayout mainGroupLayout = new GroupLayout(getContentPane());
        getContentPane().setLayout(mainGroupLayout);
        mainGroupLayout.setHorizontalGroup(
            mainGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(mainGroupLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(progressBarPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(mainGroupLayout.createSequentialGroup()
                        .addComponent(connectionStatusPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(gapPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        mainGroupLayout.setVerticalGroup(
            mainGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(mainGroupLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(connectionStatusPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(gapPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBarPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }

    private JMenuItem buildAboutMenuItem() {

        JMenuItem aboutMenuItem = new JMenuItem();
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(event -> {
            AboutForm aboutForm = new AboutForm();
            aboutForm.setLocationRelativeTo(this);
            aboutForm.setVisible(true);
        });
        return aboutMenuItem;
    }

    private JMenu buildHelpMenu() {
        JMenu helpMenu = new JMenu();
        helpMenu.setText("Help");
        helpMenu.add(buildAboutMenuItem());
        return helpMenu;
    }

    private void buildGapGroupLayout(JPanel gapPanel) {

        GroupLayout gapGroupLayout = new GroupLayout(gapPanel);
        gapPanel.setLayout(gapGroupLayout);
        gapGroupLayout.setHorizontalGroup(
            gapGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        gapGroupLayout.setVerticalGroup(
            gapGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 64, Short.MAX_VALUE)
        );
    }

    private void addWindowSettings() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Loki Render - grunt");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitQuery();
            }
        });
    }

    private void buildProgressBarLayout(JPanel progressBarPanel, JProgressBar progressBar) {

        GroupLayout progressBarGroupLayout = new GroupLayout(progressBarPanel);
        progressBarPanel.setLayout(progressBarGroupLayout);
        progressBarGroupLayout.setHorizontalGroup(
            progressBarGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(progressBarGroupLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 385, Short.MAX_VALUE)
                .addContainerGap())
        );
        progressBarGroupLayout.setVerticalGroup(
            progressBarGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, progressBarGroupLayout.createSequentialGroup()
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }

    private void buildConnectionStatusLayout(JPanel connectionStatusPanel, JLabel connectionLabelCaption, JLabel statusLabelCaption) {

        GroupLayout connectionStatusGroupLayout = new GroupLayout(connectionStatusPanel);
        connectionStatusPanel.setLayout(connectionStatusGroupLayout);
        connectionStatusGroupLayout.setHorizontalGroup(
            connectionStatusGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(connectionStatusGroupLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(connectionStatusGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(statusLabelCaption, GroupLayout.Alignment.TRAILING)
                    .addComponent(connectionLabelCaption, GroupLayout.Alignment.TRAILING))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(connectionStatusGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(connectionLabelMessage)
                    .addComponent(statusLabelMessage))
                .addContainerGap(65, Short.MAX_VALUE))
        );
        connectionStatusGroupLayout.setVerticalGroup(
            connectionStatusGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, connectionStatusGroupLayout.createSequentialGroup()
                .addContainerGap(16, Short.MAX_VALUE)
                .addGroup(connectionStatusGroupLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addGroup(connectionStatusGroupLayout.createSequentialGroup()
                        .addComponent(connectionLabelMessage)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(statusLabelMessage))
                    .addGroup(connectionStatusGroupLayout.createSequentialGroup()
                        .addComponent(connectionLabelCaption)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(statusLabelCaption)))
                .addContainerGap())
        );
    }

    private void exitQuery() {
        boolean quit = true;
        if (grunt.isBusy()) {
            int result = JOptionPane.showConfirmDialog(this,
                    "A task is currently running. If you quit now, the\n" +
                    "task will be aborted and progress will be lost.",
                    "Abort and Quit?",
                    JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != 0) {
                quit = false;
            }
        }
        if (quit) {
            grunt.signalShutdown();
            preferencesForm.dispose();
            dispose();
        }
    }

    private void abort() {
        boolean quit = true;
        if (grunt.isBusy()) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Progress on the current task will be lost. Are you sure?",
                    "Abort and quit?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != 0) {
                quit = false;
            }
        }
        if (quit) {
            grunt.abortCurrentTask(TaskStatus.LOCAL_ABORT);
            preferencesForm.dispose();
            dispose();
        }

    }

    public void setConnectionLabelMessage(String text) {
        connectionLabelMessage.setText(text);
    }
}
