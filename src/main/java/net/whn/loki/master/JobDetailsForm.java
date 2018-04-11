package net.whn.loki.master;


import net.whn.loki.common.LokiForm;
import net.whn.loki.common.Task;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class JobDetailsForm extends LokiForm {

    private JPanel generalPanel;

    private JLabel typeCaption;
    private JLabel type;

    private JLabel statusCaption;
    private JLabel status;

    private JLabel taskRangeCaption;
    private JLabel firstFrameLabel;
    private JLabel toFrameLabel;
    private JLabel lastFrameLabel;
    private JLabel renderingStepLabel;

    private JLabel projectFileCaption;
    private JLabel projectFile;

    private JLabel runnableBlendFileNameCaption;
    private JLabel runnableBlendFileName;

    private JLabel outputFolderCaption;
    private JLabel outputFolder;

    private JLabel outputFilePrefixCaption;
    private JLabel outputFilePrefix;

    private JLabel tileRenderingCaption;
    private JLabel tileRendering;

    private JLabel autoFileTransferCaption;
    private JLabel autoFileTransfer;

    private JLabel enabledAutoRunScriptsCaption;
    private JLabel enabledAutoRunScripts;

    private JTextField taskCommandLine;
    private JLabel taskCommandLineCaption;

    private JPanel taskTallyPanel;

    private JLabel readyStatusResultCaption;
    private JLabel readyStatusResult;

    private JLabel runningStatusResultCaption;
    private JLabel runningStatusResult;

    private JLabel doneStatusResultCaption;
    private JLabel doneStatusResult;

    private JLabel failedStatusResultCaption;
    private JLabel failedStatusResult;

    private JPanel bottomPanel;
    private JLabel generatedViewTimeCaption;
    private JLabel generatedViewTime;
    private JButton closeButton;

    private JPanel selectedTaskPanel;
    private JScrollPane blenderProcessOutputScrollPane;
    private JComboBox<String> blenderProcessOutputComboBox;
    private JTextArea blenderProcessOutputTextArea;

    private JScrollPane taskListScrollPane;
    private JLabel taskListCaption;
    private JTable taskListTabel;

    private final Job job;
    private final TasksModel tasksModel;
    private final Task[] tasks;

    private JLabel infoCaption;
    private JScrollPane infoScrollPane;

    public JobDetailsForm(Job job) {

        this.job = job;
        tasks = job.getTasks();
        tasksModel = new TasksModel(job);

        initComponents();
    }

    private String getStatus() {

        String status = null;

        switch (job.getStatus()) {

            case REMAINING_AND_STOPPED:
                status = "ready";
                break;
            case REMAINING_AND_TASKS_RUNNING:
            case ALL_ASSIGNED_AND_RUNNING:
                status = "running";
                break;
            case ALL_TASKS_FINISHED_OR_ABORTED:
                status = "done";
                break;
        }
        return status;
    }

    private String generateViewTime() {
        return LocalDateTime.now()
                .format(
                        DateTimeFormatter
                                .ofLocalizedDateTime(FormatStyle.MEDIUM)
                                .withLocale(Locale.UK)
                );
    }

    private void setTaskCommandLineBlenderProcessOutput() {

        String txt = "***select a task in the Task list***";
        String commandLine = "";
        int row = taskListTabel.getSelectedRow();
        if (row != -1) {    //we have a selected row
            Task task = tasks[row];
            if (blenderProcessOutputComboBox.getSelectedIndex() == 0) { //stdout
                txt = task.getStdout();
            } else if (blenderProcessOutputComboBox.getSelectedIndex() == 1) {  //errout
                txt = task.getErrOut();
            }

            String[] tokens = task.getTaskCL();
            if (tokens != null) {
                for (String token : tokens) {
                    commandLine += token + " ";
                }
            } else {
                commandLine = "";
            }
        }

        blenderProcessOutputTextArea.setText(txt);
        taskCommandLine.setText(commandLine);
    }

    private void initComponents() {

        buildMainGroupLayout2();
        setTitle("Job details for '" + job.getJobName() + "'");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
    }

    private JScrollPane buildInfoScrollPane() {

        infoCaption = new JLabel("Info");

        infoScrollPane = new JScrollPane();
        infoScrollPane.setViewportView(buildInfoContent());
        return infoScrollPane;
    }

    private Component buildInfoContent() {

        JPanel infoPane = new JPanel();
        infoPane.setLayout(new BoxLayout(infoPane, BoxLayout.Y_AXIS));
        infoPane.add(buildGeneralPanel());
        infoPane.add(buildTaskTallyPanel());
        infoPane.add(buildSelectedTaskPanel());
        return infoPane;
    }

    private JScrollPane buildTaskListScrollPane() {

        taskListCaption = new JLabel("Task list");

        taskListScrollPane = new JScrollPane();
        taskListScrollPane.setViewportView(buildTaskListTable());
        return taskListScrollPane;
    }

    private Component buildTaskListTable() {

        taskListTabel = new JTable();
        taskListTabel.setModel(tasksModel);
        taskListTabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                setTaskCommandLineBlenderProcessOutput();
            }
        });
        taskListTabel.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                setTaskCommandLineBlenderProcessOutput();
            }
        });
        taskListTabel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskListTabel.getColumnModel().getColumn(0).setMaxWidth(60);
        taskListTabel.getColumnModel().getColumn(1).setMaxWidth(40);
        return taskListTabel;
    }

    private void buildMainGroupLayout2() {

        buildBottomPanel();
        buildInfoScrollPane();
        buildTaskListScrollPane();

        final int WIDTH = 650;
        final int HEIGHT = 650;

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                        .addComponent(infoCaption)
                                                        .addComponent(infoScrollPane, GroupLayout.DEFAULT_SIZE, WIDTH, Short.MAX_VALUE)
                                                )
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                        .addComponent(taskListCaption)
                                                        .addComponent(taskListScrollPane, GroupLayout.DEFAULT_SIZE, WIDTH, Short.MAX_VALUE)
                                                )
                                                .addContainerGap()
                                        )
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(bottomPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                                        .addComponent(taskListCaption)
                                        .addComponent(infoCaption)
                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(taskListScrollPane, GroupLayout.DEFAULT_SIZE, HEIGHT, Short.MAX_VALUE)
                                        .addComponent(infoScrollPane, GroupLayout.DEFAULT_SIZE, HEIGHT, Short.MAX_VALUE)
                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(bottomPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                )
                                .addContainerGap()
                        )
        );
    }

    private void buildBottomPanel() {

        generatedViewTimeCaption = new JLabel("View generated:");
        generatedViewTime = new JLabel(generateViewTime());

        closeButton = new JButton("Close");
        closeButton.addActionListener((event) -> dispose());

        bottomPanel = new JPanel();
        GroupLayout bottomPanelGroupLayout = new GroupLayout(bottomPanel);
        bottomPanel.setLayout(bottomPanelGroupLayout);
        bottomPanelGroupLayout.setHorizontalGroup(
                bottomPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(bottomPanelGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(generatedViewTimeCaption)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(generatedViewTime)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(closeButton, GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        bottomPanelGroupLayout.setVerticalGroup(
                bottomPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(bottomPanelGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(bottomPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(generatedViewTimeCaption)
                                        .addComponent(generatedViewTime)
                                        .addComponent(closeButton))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }

    private JPanel buildTaskTallyPanel() {

        readyStatusResultCaption = new JLabel("ready:");
        readyStatusResult = new JLabel(getStringValue(job.getReadyTasks()));

        runningStatusResultCaption = new JLabel("running:");
        runningStatusResult = new JLabel(getStringValue(job.getRunningTasks()));

        doneStatusResultCaption = new JLabel("done:");
        doneStatusResult = new JLabel(getStringValue(job.getDoneTasks()));

        failedStatusResultCaption = new JLabel("failed:");
        failedStatusResult = new JLabel(getStringValue(job.getFailedTasks()));

        taskTallyPanel = new JPanel();
        taskTallyPanel.setBorder(BorderFactory.createTitledBorder("Task tally"));

        GroupLayout pnlTaskTallyLayout = new GroupLayout(taskTallyPanel);
        taskTallyPanel.setLayout(pnlTaskTallyLayout);
        pnlTaskTallyLayout.setHorizontalGroup(
                pnlTaskTallyLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlTaskTallyLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(pnlTaskTallyLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(doneStatusResultCaption)
                                        .addComponent(runningStatusResultCaption)
                                        .addComponent(failedStatusResultCaption)
                                        .addGroup(GroupLayout.Alignment.LEADING, pnlTaskTallyLayout.createSequentialGroup()
                                                .addGap(98, 98, 98)
                                                .addComponent(readyStatusResultCaption)))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlTaskTallyLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(readyStatusResult)
                                        .addComponent(runningStatusResult)
                                        .addComponent(doneStatusResult)
                                        .addComponent(failedStatusResult))
                                .addContainerGap(45, Short.MAX_VALUE)
                        )
        );
        pnlTaskTallyLayout.setVerticalGroup(
                pnlTaskTallyLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlTaskTallyLayout.createSequentialGroup()
                                .addGroup(pnlTaskTallyLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(readyStatusResultCaption)
                                        .addComponent(readyStatusResult))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlTaskTallyLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(runningStatusResultCaption)
                                        .addComponent(runningStatusResult))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlTaskTallyLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(doneStatusResultCaption)
                                        .addComponent(doneStatusResult))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlTaskTallyLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(failedStatusResultCaption)
                                        .addComponent(failedStatusResult)))
        );
        return taskTallyPanel;
    }

    private JPanel buildSelectedTaskPanel() {

        taskCommandLineCaption = new JLabel("task command line:");

        taskCommandLine = new JTextField();
        taskCommandLine.setEditable(false);

        blenderProcessOutputComboBox = new JComboBox<>();
        ((JLabel)blenderProcessOutputComboBox.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        blenderProcessOutputComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"stdout", "errout"}));
        blenderProcessOutputComboBox.addActionListener((event) -> setTaskCommandLineBlenderProcessOutput());

        blenderProcessOutputScrollPane = new JScrollPane();
        blenderProcessOutputTextArea = new JTextArea();
        blenderProcessOutputScrollPane.setViewportView(blenderProcessOutputTextArea);

        selectedTaskPanel = new JPanel();
        selectedTaskPanel.setBorder(BorderFactory.createTitledBorder("Selected task"));

        GroupLayout selectedTaskPanelGroupLayout = new GroupLayout(selectedTaskPanel);
        selectedTaskPanel.setLayout(selectedTaskPanelGroupLayout);
        selectedTaskPanelGroupLayout.setHorizontalGroup(
                selectedTaskPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, selectedTaskPanelGroupLayout.createSequentialGroup()
                                .addGroup(selectedTaskPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(blenderProcessOutputScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 501, Short.MAX_VALUE)
                                        .addComponent(taskCommandLine, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 501, Short.MAX_VALUE)
                                        .addComponent(taskCommandLineCaption, GroupLayout.Alignment.LEADING)
                                        .addComponent(blenderProcessOutputComboBox, GroupLayout.Alignment.LEADING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                )
                        )
        );
        selectedTaskPanelGroupLayout.setVerticalGroup(
                selectedTaskPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(selectedTaskPanelGroupLayout.createSequentialGroup()
                                .addComponent(taskCommandLineCaption)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(taskCommandLine, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(blenderProcessOutputComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(blenderProcessOutputScrollPane, GroupLayout.PREFERRED_SIZE, 182, Short.MAX_VALUE)
                        )
        );
        return selectedTaskPanel;
    }

    private JPanel buildGeneralPanel() {

        typeCaption = new JLabel("type:");
        type = new JLabel(job.getJobType().toString());

        statusCaption = new JLabel("status:");
        status = new JLabel(getStatus());

        taskRangeCaption = new JLabel("task range:");
        firstFrameLabel = new JLabel(getStringValue(job.getFirstTask()));
        toFrameLabel = new JLabel("to");
        lastFrameLabel = new JLabel(getStringValue(job.getLastTask()));
        renderingStepLabel = new JLabel("step: " + job.getRenderingStep());

        projectFileCaption = new JLabel("project file:");
        projectFile = new JLabel(job.getProjectFile().getName());

        runnableBlendFileNameCaption = new JLabel("runnable blend file:");
        runnableBlendFileName = new JLabel(job.getRunnableBenderFileName());

        outputFolderCaption = new JLabel("output directory:");
        outputFolder = new JLabel(job.getOutputFolder().getAbsolutePath());

        outputFilePrefixCaption = new JLabel("output file prefix:");
        outputFilePrefix = new JLabel(job.getFilePrefix());

        tileRenderingCaption = new JLabel("tile rendering:");
        tileRendering = new JLabel(job.getTileStr());

        autoFileTransferCaption = new JLabel("auto file transfer:");
        autoFileTransfer = new JLabel(job.isAutoFileTransfer() ? "enabled" : "disabled");

        enabledAutoRunScriptsCaption = new JLabel("enabled auto run scripts:");
        enabledAutoRunScripts = new JLabel(job.isEnabledAutoRunScripts() ? "enabled" : "disabled");

        generalPanel = new JPanel();
        generalPanel.setBorder(BorderFactory.createTitledBorder("General"));

        GroupLayout generalPanelGroupLayout = new GroupLayout(generalPanel);
        generalPanel.setLayout(generalPanelGroupLayout);
        generalPanelGroupLayout.setHorizontalGroup(
                generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(generalPanelGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                                .addComponent(typeCaption)
                                                .addComponent(statusCaption)
                                                .addComponent(taskRangeCaption)
                                                .addComponent(projectFileCaption)
                                                .addComponent(runnableBlendFileNameCaption)
                                                .addComponent(outputFolderCaption)
                                                .addComponent(outputFilePrefixCaption, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addComponent(enabledAutoRunScriptsCaption)
                                        .addComponent(autoFileTransferCaption)
                                        .addComponent(tileRenderingCaption))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(type)
                                        .addComponent(status)
                                        .addGroup(generalPanelGroupLayout.createSequentialGroup()
                                                .addComponent(firstFrameLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(toFrameLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lastFrameLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(renderingStepLabel)
                                        )
                                        .addComponent(projectFile)
                                        .addComponent(runnableBlendFileName)
                                        .addComponent(outputFolder)
                                        .addComponent(outputFilePrefix)
                                        .addComponent(tileRendering)
                                        .addComponent(autoFileTransfer)
                                        .addComponent(enabledAutoRunScripts)
                                ).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )
        );
        generalPanelGroupLayout.setVerticalGroup(
                generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(generalPanelGroupLayout.createSequentialGroup()
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(typeCaption)
                                        .addComponent(type))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(statusCaption)
                                        .addComponent(status))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(taskRangeCaption)
                                        .addComponent(firstFrameLabel)
                                        .addComponent(toFrameLabel)
                                        .addComponent(lastFrameLabel)
                                        .addComponent(renderingStepLabel))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(projectFileCaption)
                                        .addComponent(projectFile))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(runnableBlendFileNameCaption)
                                        .addComponent(runnableBlendFileName))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(outputFolderCaption)
                                        .addComponent(outputFolder))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(outputFilePrefixCaption)
                                        .addComponent(outputFilePrefix))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(tileRenderingCaption)
                                        .addComponent(tileRendering))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(autoFileTransferCaption)
                                        .addComponent(autoFileTransfer))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(enabledAutoRunScriptsCaption)
                                        .addComponent(enabledAutoRunScripts)
                                ).addContainerGap(GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
                        )
        );
        return generalPanel;
    }

    private String getStringValue(int intValue) {
        return Integer.toString(intValue);
    }

    private class TasksModel extends AbstractTableModel {

        private final String[] columnHeaders;
        private final Job job;
        private final Task[] tasks;

        TasksModel(Job job) {
            this.job = job;
            tasks = job.getTasks();
            columnHeaders = new String[]{
                    "frame",
                    "tile",
                    "grunt",
                    "status",
                    "time"
            };
        }

        @Override
        public String getColumnName(int col) {
            return columnHeaders[col].toString();
        }

        @Override
        public int getRowCount() {
            return tasks.length;
        }

        @Override
        public int getColumnCount() {
            return columnHeaders.length;
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return getStringValue(tasks[row].getFrame());
                case 1:
                    return tasks[row].getTile() == -1 ? "n/a" : getStringValue(tasks[row].getTile());
                case 2:
                    return tasks[row].getGruntName();
                case 3:
                    return tasks[row].getStatus().toString().toLowerCase();
                case 4:
                    return tasks[row].getTaskTime();
                default:
                    return "";
            }
        }
    }
}
