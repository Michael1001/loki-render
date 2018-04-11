package net.whn.loki.master;

import net.whn.loki.common.LokiForm;
import net.whn.loki.common.configs.Config;
import net.whn.loki.master.utils.CheckBoxListener;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * Provides a form for creating a new job
 */
public class AddJobForm extends LokiForm {

    private JButton projectFileBrowseButton;
    private JButton outputFolderBrowseButton;
    private JButton cancelButton;
    private JCheckBox autoFileTransferComboBox;
    private JCheckBox keepFoldersStructureCheckBox;
    private JCheckBox enableAutoRunScriptsCheckBox;
    private JCheckBox enableTileCheckBox;
    private JComboBox jobTypeComboBox;
    private JComboBox comboboxTileMultiplier;
    private JPanel generalPanel;

    private JPanel tileRenderingPanel;
    private JButton saveButton;
    private JobNameTextField jobNameTextField = new JobNameTextField();
    private JTextField outputFilePrefixTextField;
    private JTextField steppedRenderingTextField;
    private MandatoryFileTextField projectFileTextField = new MandatoryFileTextField("specify the full path to the project file here", true);
    private MandatoryFileTextField outputFolderTextField = new MandatoryFileTextField("the output directory where rendered frames will be placed", false);
    private MandatoryIntegerTextField firstFrameTextField = new MandatoryIntegerTextField("the first frame in the frame range to be rendered (e.g. '1')", true);
    private MandatoryIntegerTextField lastFrameTextField = new MandatoryIntegerTextField("the last frame in the frame range to be rendered (e.g. '200')");
    private CommandLineScriptTextField commandLineScriptTextField = new CommandLineScriptTextField();
    private List<String> allowedCommandLineArgFormats = Arrays.asList("TIFF", "PNG", "TGA", "RAWTGA", "JPEG", "EXR");
    /**
     * Could be either a simple single blend file, or even an archive with blend files
     */
    private FileExtensions selectedProjectFileExtension;
    private String runnableBlendFileName;
    private CheckBoxListener checkBoxListener;
    private Dimension preferredButtonSize = new Dimension(85, 25);
    private Dimension preferredTextFieldSize = new Dimension(85, 21);
    private int prefferedTextFieldHeight = 21;
    private final MasterForm masterForm;
    private final Config config;

    private RequiredJLabel projectFileLabel = new RequiredJLabel("Project File", "*");
    private RequiredJLabel firstFrameLabel = new RequiredJLabel("First Frame", "*");
    private RequiredJLabel outputFolderLabel = new RequiredJLabel("Output Directory", "*");
    private RequiredJLabel outputFilePrefixLabel = new RequiredJLabel("Output File Prefix");
    private RequiredJLabel comboboxTileMultiplierCaption = new RequiredJLabel("Multiplier");
    private RequiredJLabel lastFrameLabel = new RequiredJLabel("Last Frame", "*");
    private RequiredJLabel jobNameLabel = new RequiredJLabel("Name", "*");
    private RequiredJLabel jobTypeLabel = new RequiredJLabel("Type");
    private RequiredJLabel commandLineScriptLabel = new RequiredJLabel("Command line script");
    private RequiredJLabel steppedRenderingLabel = new RequiredJLabel("Stepped");
    private JLabel tileMultiplierResultLabel = new JLabel();
    private JobsModel jobsModel;
    private Border errorBorder = BorderFactory.createLineBorder(Color.PINK);

    public AddJobForm(MasterForm masterForm, JobsModel jobsModel) {

        setForeground(Color.RED);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.masterForm = masterForm;
        this.config = masterForm.getCfg();
        this.jobsModel = jobsModel;

        initComponents();

        // need special checking first time when this class is just created, because of a bug in Swing
        String projectFileFromConfig = config.getProjectFile().toString().trim();
        if (!projectFileFromConfig.isEmpty()) {
            File file = new File(projectFileFromConfig);

            // do this because if project file is an archive, then should be selected runnable file
            String fileName = file.getName();
            if (file.isFile() && file.exists() && file.canRead()) {
                if (FileExtensions.isBlendFile(fileName)) {
                    runnableBlendFileName = fileName;
                    projectFileTextField.presetText(projectFileFromConfig);
                } else {
                    runnableBlendFileName = config.getRunnableBlendFileName();
                    projectFileTextField.presetText(projectFileFromConfig);
                }
            }
        }

        outputFolderTextField.setText(config.getOutputFolder().toString());

        outputFilePrefixTextField.setText(config.getFilePrefix());
        comboboxTileMultiplier.setSelectedIndex(2);
        autoFileTransferComboBox.setSelected(config.getAutoFileHandling());
        updateTileMultiplierResultLabel();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        generalPanel = new JPanel();
        jobTypeComboBox = new JComboBox();
        projectFileBrowseButton = new JButton();
        outputFolderBrowseButton = new JButton();
        outputFilePrefixTextField = new JTextField();
        steppedRenderingTextField = new JTextField("10");
        autoFileTransferComboBox = new JCheckBox();

        cancelButton = new JButton();
        saveButton = new JButton();
        tileRenderingPanel = new JPanel();
        enableTileCheckBox = new JCheckBox();
        comboboxTileMultiplier = new JComboBox();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("New Job");

        generalPanel.setBorder(BorderFactory.createTitledBorder("General"));

        jobTypeComboBox.setModel(new DefaultComboBoxModel(new String[]{"Blender"}));
        jobTypeComboBox.setToolTipText("select the job type here");

        projectFileBrowseButton.setText("Browse");
        projectFileBrowseButton.setName("projectFileBrowseButton"); // NOI18N
        projectFileBrowseButton.addActionListener(event -> selectAProjectFile());

        outputFolderBrowseButton.setText("Browse");
        outputFolderBrowseButton.addActionListener(event -> selectAnOutputFolder());

        outputFilePrefixTextField.setToolTipText("specify a file prefix for the rendered frames (e.g. \"scene1_\" would give \"scene1_0024.jpg\", etc.)");

        autoFileTransferComboBox.setText("Enable automatic file transfer");
        autoFileTransferComboBox.setToolTipText("Uncheck this if you have setup a network share with project files that all nodes can access.\nUseful for large projects with lots of texture files, etc.");
        autoFileTransferComboBox.addActionListener(event -> cbxAutoFileTransferActionPerformed());

        keepFoldersStructureCheckBox = new JCheckBox("Keep folders structure", true);
        keepFoldersStructureCheckBox.setToolTipText("Check this if you want to keep folders structure in the output folder like on the Grunt machine");

        enableAutoRunScriptsCheckBox = new JCheckBox("Enable Auto-Run Scripts", config.isEnabledAutoRunScripts());

        GroupLayout generalPanelLayout = new GroupLayout(generalPanel);
        generalPanel.setLayout(generalPanelLayout);
        generalPanelLayout.setHorizontalGroup(
                generalPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(generalPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(buildCaptionGroup(generalPanelLayout))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(buildFieldsGroup(generalPanelLayout))
                                .addContainerGap()
                        )
        );

        generalPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[]{firstFrameTextField, lastFrameTextField, steppedRenderingTextField});
        generalPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[]{jobNameTextField, outputFilePrefixTextField});
        generalPanelLayout.setVerticalGroup(
                generalPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(buildVerticalComponentsGroup(generalPanelLayout))
        );

        cancelButton.setText("Cancel");
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.setSize(preferredButtonSize);
        cancelButton.addActionListener(event -> dispose());

        saveButton.setText("Save");
        saveButton.setSize(preferredButtonSize);
        saveButton.setName("saveButton"); // NOI18N
        saveButton.addActionListener(event -> saveButtonActionPerformed());

        tileRenderingPanel.setBorder(BorderFactory.createTitledBorder("Tile rendering"));
        tileRenderingPanel.setToolTipText("Tile rendering splits a frame into separate parts for parallel rendering of tiles.");

        enableTileCheckBox.setText("Enabled");
        enableTileCheckBox.addActionListener(event -> enableTileComponents(enableTileCheckBox.isSelected()));

        comboboxTileMultiplier.setModel(new DefaultComboBoxModel(new String[]{"2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"}));
        comboboxTileMultiplier.setToolTipText("select a multiplier to specify how many tiles parts will be used");
        comboboxTileMultiplier.addActionListener(event -> updateTileMultiplierResultLabel());
        comboboxTileMultiplier.setEnabled(false); // by default "Tile rendering" is not checked

        comboboxTileMultiplierCaption.setEnabled(false);

        tileMultiplierResultLabel.setToolTipText("the total number of tiles parts");
        tileMultiplierResultLabel.setEnabled(false); // by default "Tile rendering" is not checked

        GroupLayout pnlTileRenderingLayout = new GroupLayout(tileRenderingPanel);
        tileRenderingPanel.setLayout(pnlTileRenderingLayout);
        pnlTileRenderingLayout.setHorizontalGroup(
                pnlTileRenderingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlTileRenderingLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(pnlTileRenderingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(enableTileCheckBox)
                                        .addGroup(pnlTileRenderingLayout.createSequentialGroup()
                                                .addComponent(comboboxTileMultiplierCaption)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(comboboxTileMultiplier, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(tileMultiplierResultLabel, GroupLayout.PREFERRED_SIZE, 134, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(282, Short.MAX_VALUE))
        );
        pnlTileRenderingLayout.setVerticalGroup(
                pnlTileRenderingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlTileRenderingLayout.createSequentialGroup()
                                .addComponent(enableTileCheckBox)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlTileRenderingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(tileMultiplierResultLabel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(pnlTileRenderingLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(comboboxTileMultiplierCaption)
                                                .addComponent(comboboxTileMultiplier, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                .addGap(12, 12, 12))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(tileRenderingPanel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(cancelButton, GroupLayout.PREFERRED_SIZE, 102, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(saveButton, GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(generalPanel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(generalPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tileRenderingPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(saveButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cancelButton))
                                .addContainerGap())
        );

        pack();
    }

    /**
     * Performed when enableTileCheckBox is checked/unchecked
     *
     * @param isEnabled
     */
    private void enableTileComponents(boolean isEnabled) {
        comboboxTileMultiplierCaption.setEnabled(isEnabled);
        comboboxTileMultiplier.setEnabled(isEnabled);
        tileMultiplierResultLabel.setEnabled(isEnabled);
    }

    private GroupLayout.SequentialGroup buildVerticalComponentsGroup(GroupLayout generalPanelLayout) {
        GroupLayout.SequentialGroup group = generalPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jobTypeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jobTypeLabel)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jobNameLabel)
                        .addComponent(jobNameTextField, prefferedTextFieldHeight, prefferedTextFieldHeight, prefferedTextFieldHeight)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(projectFileTextField, prefferedTextFieldHeight, prefferedTextFieldHeight, prefferedTextFieldHeight)
                        .addComponent(projectFileLabel)
                        .addComponent(projectFileBrowseButton)
                )
                .addGap(7, 7, 7)
                .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(outputFolderTextField, prefferedTextFieldHeight, prefferedTextFieldHeight, prefferedTextFieldHeight)
                        .addComponent(outputFolderLabel)
                        .addComponent(outputFolderBrowseButton)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(outputFilePrefixTextField, prefferedTextFieldHeight, prefferedTextFieldHeight, prefferedTextFieldHeight)
                        .addComponent(outputFilePrefixLabel)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(firstFrameLabel)
                        .addComponent(firstFrameTextField, prefferedTextFieldHeight, prefferedTextFieldHeight, prefferedTextFieldHeight)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lastFrameLabel)
                        .addComponent(lastFrameTextField, prefferedTextFieldHeight, prefferedTextFieldHeight, prefferedTextFieldHeight)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(steppedRenderingLabel)
                        .addComponent(steppedRenderingTextField, prefferedTextFieldHeight, prefferedTextFieldHeight, prefferedTextFieldHeight)
                );


        if (config.isEnabledCommandLineScripts()) {
            group.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(commandLineScriptLabel)
                            .addComponent(commandLineScriptTextField, prefferedTextFieldHeight, prefferedTextFieldHeight, prefferedTextFieldHeight)
                    );
        }
        group.addGap(18, 18, 18)
                .addComponent(autoFileTransferComboBox)
                .addComponent(keepFoldersStructureCheckBox);

        if (config.isEnabledAutoRunScripts()) {
            group.addComponent(enableAutoRunScriptsCheckBox);
        }
        group.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);

        return group;
    }

    private GroupLayout.ParallelGroup buildFieldsGroup(GroupLayout generalPanelLayout) {
        GroupLayout.ParallelGroup group = generalPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(generalPanelLayout.createSequentialGroup()
                        .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(steppedRenderingTextField, GroupLayout.PREFERRED_SIZE, 84, GroupLayout.PREFERRED_SIZE)
                                .addComponent(lastFrameTextField, GroupLayout.PREFERRED_SIZE, 84, GroupLayout.PREFERRED_SIZE)
                                .addComponent(firstFrameTextField, GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE)
                                .addComponent(outputFilePrefixTextField, GroupLayout.PREFERRED_SIZE, 171, GroupLayout.PREFERRED_SIZE)
                                .addComponent(jobNameTextField, GroupLayout.PREFERRED_SIZE, 175, GroupLayout.PREFERRED_SIZE)
                                .addComponent(jobTypeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(projectFileTextField, GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                                .addComponent(outputFolderTextField)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(projectFileBrowseButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(outputFolderBrowseButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )
                )
                .addComponent(autoFileTransferComboBox)
                .addComponent(keepFoldersStructureCheckBox);
        if (config.isEnabledAutoRunScripts()) {
            group.addComponent(enableAutoRunScriptsCheckBox);
        }

        if (config.isEnabledCommandLineScripts()) {
            group.addGroup(generalPanelLayout.createSequentialGroup()
                    .addComponent(commandLineScriptTextField, GroupLayout.PREFERRED_SIZE, 418, Short.MAX_VALUE)
            );
        }

        return group;
    }

    private GroupLayout.ParallelGroup buildCaptionGroup(GroupLayout generalPanelLayout) {
        GroupLayout.ParallelGroup group = generalPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(steppedRenderingLabel, GroupLayout.Alignment.TRAILING)
                .addComponent(lastFrameLabel, GroupLayout.Alignment.TRAILING)
                .addComponent(firstFrameLabel, GroupLayout.Alignment.TRAILING)
                .addComponent(outputFilePrefixLabel, GroupLayout.Alignment.TRAILING)
                .addComponent(outputFolderLabel, GroupLayout.Alignment.TRAILING)
                .addComponent(projectFileLabel, GroupLayout.Alignment.TRAILING)
                .addComponent(jobNameLabel, GroupLayout.Alignment.TRAILING)
                .addComponent(jobTypeLabel, GroupLayout.Alignment.TRAILING);
        if (config.isEnabledCommandLineScripts()) {
            group.addComponent(commandLineScriptLabel, GroupLayout.Alignment.TRAILING);
        }
        return group;
    }

    /**
     * creates a new job object with the given values and passes it to the masterForm
     * Button Save, will be enabled, only if all the fields passed validations
     * <p>
     * Project file, and output folder, could be set:
     * - read from config file;
     * - selected via JFileChooser, from file system;
     * - written by hand (what needs );
     */
    private void saveButtonActionPerformed() {

        // case when project file was set from Config file
        File file = projectFileTextField.getFile();
        if (runnableBlendFileName == null) {
            checkChoosedProjectFile(file, file.getName());
        }

        if (runnableBlendFileName != null) {
            int renderigStep;
            try {
                renderigStep = Integer.valueOf(steppedRenderingTextField.getText());
                if (renderigStep <= 0) {
                    renderigStep = 1;
                }
            } catch (Exception e) {
                renderigStep = 1;
            }

            if (selectedProjectFileExtension == null) {
                switch (FileExtensions.getByFileName(file.getName())) {

                    case ZIP:
                        selectedProjectFileExtension = FileExtensions.ZIP;
                        break;
                    case BLEND:
                        selectedProjectFileExtension = FileExtensions.BLEND;
                }
            }

            masterForm.addJob(new JobFormInput(
                            jobTypeComboBox.getSelectedItem().toString(),
                            jobNameTextField.getText(),
                            projectFileTextField.getText(),
                            outputFolderTextField.getText(),
                            outputFilePrefixTextField.getText(),
                            firstFrameTextField.getValue(),
                            lastFrameTextField.getValue(),
                            enableTileCheckBox.isSelected(),
                            Integer.parseInt((String) comboboxTileMultiplier.getSelectedItem()),
                            autoFileTransferComboBox.isSelected(),
                            keepFoldersStructureCheckBox.isSelected(),
                            getCommandLineScriptArgs(),
                            selectedProjectFileExtension,
                            runnableBlendFileName,
                            renderigStep,
                            enableAutoRunScriptsCheckBox.isSelected()
                    )
            );
            dispose();
            masterForm.setEnabled(true);

            //'remember' paths for next dialog run
            Config config = masterForm.getCfg();
            config.setProjectFile(projectFileTextField.getFile());
            config.setOutputFolder(outputFolderTextField.getFile());
            config.setFilePrefix(outputFilePrefixTextField.getText());
            config.setRunnableBlendFileName(runnableBlendFileName);
        }
    }

    private List<String> getCommandLineScriptArgs() {
        List<String> commandLineScriptArgs = new ArrayList<>();
        if (config.isEnabledCommandLineScripts()) {
            commandLineScriptArgs.addAll(Arrays.asList(commandLineScriptTextField.getText().split(" ")));
        }
        return commandLineScriptArgs;
    }

    private void selectAProjectFile() {

        JFileChooser fileChooser = new JFileChooser(masterForm.getCfg().getProjectFile());
        fileChooser.setDialogTitle("Select a project file");
        addFileFilters(fileChooser);

        if (fileChooser.showDialog(outputFolderLabel, "Select") == JFileChooser.APPROVE_OPTION) {

            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName();
            checkChoosedProjectFile(selectedFile, fileName);
        } else {
            resetVariables();
        }
    }

    private void resetVariables() {
        runnableBlendFileName = null;
        selectedProjectFileExtension = null;
    }

    private boolean checkChoosedProjectFile(File selectedFile, String fileName) {

        switch (FileExtensions.getByFileName(fileName)) {

            case ZIP:
                if (wasSelectedRunnableBlendFiles(getBlendFileNamesFromZip(selectedFile))) {
                    projectFileTextField.setText(selectedFile.toString());
                    runnableBlendFileName = checkBoxListener.getRunnableBlendFileName();
                    selectedProjectFileExtension = FileExtensions.ZIP;
                } else {
                    runnableBlendFileName = null;
                }
                break;
//            case TAR: // TODO
//                projectFileTextField.setText(null);
//                runnableBlendFileName = null;
//                selectedProjectFileExtension = FileExtensions.TAR;
//                break;
//            case GZ: // TODO
//                projectFileTextField.setText(null);
//                runnableBlendFileName = null;
//                selectedProjectFileExtension = FileExtensions.GZ;
//                break;
            case BLEND:
                projectFileTextField.setText(selectedFile.toString());
                runnableBlendFileName = selectedFile.getName();
                selectedProjectFileExtension = FileExtensions.BLEND;
                break;
            default:
                projectFileTextField.setText(null);
                runnableBlendFileName = null;
                selectedProjectFileExtension = null;
        }
        return runnableBlendFileName != null;
    }

    /**
     * @param selectedFile - should be an zip archive
     * @return a string map like this:
     * "Link TEST.blend"" = "Rocks\Scenes\LOOKDEV\Props\Rocks\Link TEST.blend"
     * <p>
     * Assuming from start that this zip archive will have only unique blend file names, otherwise will be throwed a runtime exception.
     * The method Collectors.toMap(keyMapper, valueMapper) will throw IllegalStateException if there are duplicate keys as provided by keyMapper function.
     */
    private Map<String, String> getBlendFileNamesFromZip(File selectedFile) {

        Map<String, String> blendFileNamesMap = new LinkedHashMap<>();
        try {
            ZipFile zipFile = new ZipFile(selectedFile.getPath());

            blendFileNamesMap.putAll(
                    zipFile.stream()
                            .filter(o -> o.getName().endsWith(".blend"))
                            .map(o -> o.getName())
                            .collect(
                                    Collectors.toMap(
                                            relativePathName -> {
                                                String pathSeparator = "/";
                                                return relativePathName.contains(pathSeparator)
                                                        ? relativePathName.substring(relativePathName.lastIndexOf(pathSeparator) + 1, relativePathName.length())
                                                        : relativePathName;
                                            },
                                            relativePathName -> relativePathName
                                    )
                            )
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return blendFileNamesMap;
    }

    private boolean wasSelectedRunnableBlendFiles(Map<String, String> blendFileNamesMap) {

        JButton okButton = new JButton("OK");
        okButton.setEnabled(false);
        okButton.setPreferredSize(preferredButtonSize);
        okButton.addActionListener(e -> {
            JOptionPane pane = getOptionPane((JComponent) e.getSource());
            pane.setValue(okButton);
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(preferredButtonSize);
        cancelButton.addActionListener(e -> {
            JOptionPane pane = getOptionPane((JComponent) e.getSource());
            pane.setValue(cancelButton);
        });

        checkBoxListener = new CheckBoxListener(okButton, blendFileNamesMap);

        String message = "Which blend files to run when submitting the job ?";
        String title = "Select blend files";

        JPanel panel = new JPanel();

        GridLayout gridLayout = new GridLayout(blendFileNamesMap.size() + 1, 1);
        panel.setLayout(gridLayout);
        panel.add(new JLabel(message));

        List<JCheckBox> checkBoxes = new ArrayList<>(blendFileNamesMap.size());

        for (String blendFileName : blendFileNamesMap.keySet()) {

            JCheckBox checkBox = new JCheckBox(blendFileName);
            checkBox.addActionListener(checkBoxListener);

            checkBoxes.add(checkBox);
            panel.add(checkBox);
        }
        checkBoxListener.addCheckBoxes(checkBoxes);

        boolean wasPressedOkBtn = JOptionPane.showOptionDialog(
                null,
                panel,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{okButton, cancelButton},
                okButton
        ) == JOptionPane.YES_OPTION;

        return wasPressedOkBtn;
    }

    public static JOptionPane getOptionPane(JComponent parent) {
        JOptionPane pane = null;
        if (!(parent instanceof JOptionPane)) {
            pane = getOptionPane((JComponent) parent.getParent());
        } else {
            pane = (JOptionPane) parent;
        }
        return pane;
    }

    private void addFileFilters(JFileChooser fileChooser) {

        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new FileTypeFilter(FileExtensions.BLEND, FileExtensions.ZIP));
    }

    private void updateTileMultiplierResultLabel() {
        Integer multiplier = Integer.parseInt((String) comboboxTileMultiplier.getSelectedItem());
        String txt = multiplier.toString() + " * " + multiplier.toString() + " = " + Integer.toString(multiplier * multiplier) + " tiles";
        tileMultiplierResultLabel.setText(txt);
    }

    private void selectAnOutputFolder() {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setDialogTitle("Select an output directory");
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setAcceptAllFileFilterUsed(false);
        dirChooser.setCurrentDirectory(masterForm.getCfg().getOutputFolder());
        if (dirChooser.showDialog(outputFolderLabel, "Select") == JFileChooser.APPROVE_OPTION) {
            outputFolderTextField.setText(dirChooser.getSelectedFile().getPath());
        }
    }

    private void cbxAutoFileTransferActionPerformed() {

        if (autoFileTransferComboBox.isSelected()) {
            keepFoldersStructureCheckBox.setEnabled(true);
        } else {
            keepFoldersStructureCheckBox.setSelected(false);
            keepFoldersStructureCheckBox.setEnabled(false);
        }

        if (autoFileTransferComboBox.isSelected() && commandLineScriptTextField.isEnabledTileRendering()) {
            tileRenderingPanel.setEnabled(true);
            enableTileCheckBox.setEnabled(true);
            comboboxTileMultiplierCaption.setEnabled(true);
            comboboxTileMultiplier.setEnabled(true);
            tileMultiplierResultLabel.setEnabled(true);
        } else {
            tileRenderingPanel.setEnabled(false);
            enableTileCheckBox.setEnabled(false);
            enableTileCheckBox.setSelected(false);
            comboboxTileMultiplierCaption.setEnabled(false);
            comboboxTileMultiplier.setEnabled(false);
            tileMultiplierResultLabel.setEnabled(false);
        }
    }

    public String getRunnableBlendFileName() {
        return runnableBlendFileName;
    }

    private class CommandLineScriptTextField extends JTextField {

        private boolean valid = true;
        private boolean enabledTileRendering = true;
        private Border defaultBorder = getBorder();

        public CommandLineScriptTextField() {
            getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    checkInputs();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    checkInputs();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    checkInputs();
                }
            });
        }

        private void checkInputs() {
            String inputs = commandLineScriptTextField.getText().trim();
            if (inputs.isEmpty()) {
                tileRenderingPanel.setEnabled(true);
                enableTileCheckBox.setEnabled(true);
                valid = true;
                enabledTileRendering = true;
                setBorder(defaultBorder);
            } else {
                String[] split = inputs.split(" ");

                boolean hasAtLeastTwoArgs = split.length >= 2;
                valid = hasAtLeastTwoArgs && split[0].equals("-F") && allowedCommandLineArgFormats.contains(split[1]); // split[1]: render file format like : TIFF, PNG, etc

                setBorder(valid ? defaultBorder : errorBorder);

                enabledTileRendering = valid && !split[1].equals("EXR");
                enableTileRendering(enabledTileRendering);
            }
            saveButton.setEnabled(valid && jobNameTextField.isValid() && projectFileTextField.isValid() && outputFolderTextField.isValid() && firstFrameTextField.areValidBothFrameFields());
        }

        private void enableTileRendering(boolean isEnabled) {
            tileRenderingPanel.setEnabled(isEnabled);
            if (!isEnabled) {
                enableTileCheckBox.setSelected(false);
                enableTileComponents(false);
            }
            enableTileCheckBox.setEnabled(isEnabled);
        }

        @Override
        public boolean isValid() {
            return valid;
        }

        public boolean isEnabledTileRendering() {
            return enabledTileRendering;
        }
    }

    private class JobNameTextField extends JTextField {

        private boolean valid;
        private Border defaultBorder = getBorder();

        public JobNameTextField() {

            setPreferredSize(preferredTextFieldSize);
            setToolTipText("type a unique job name here");

            getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateSaveBtn();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateSaveBtn();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateSaveBtn();
                }

                private void updateSaveBtn() {
                    String text = jobNameTextField.getText().trim();
                    if (text.isEmpty()) {
                        saveButton.setEnabled(false);
                        valid = false;

                    } else {
                        boolean isNameUnique = jobsModel.isJobNameUnique(text);
                        valid = isNameUnique;

                        setBorder(valid ? defaultBorder : errorBorder);

                        boolean enableSaveButton = valid && projectFileTextField.isValid() && outputFolderTextField.isValid()
                                && firstFrameTextField.areValidBothFrameFields() && commandLineScriptTextField.isValid();
                        saveButton.setEnabled(enableSaveButton);
                    }

                }
            });
        }

        @Override
        public boolean isValid() {
            return valid;
        }
    }

    private class MandatoryFileTextField extends JTextField {

        private boolean valid;
        private Border defaultBorder = getBorder();
        private boolean isPresetText;

        /**
         * @param toolTipText
         * @param isUsedForProjectFile - This class is used for both project file and output folder, so there is need to differentiate them
         */
        public MandatoryFileTextField(String toolTipText, boolean isUsedForProjectFile) {

            setToolTipText(toolTipText);

            getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    if (!isPresetText) {
                        updateSaveBtn();
                    }
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    if (!isPresetText) {
                        updateSaveBtn();
                    }
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    if (!isPresetText) {
                        updateSaveBtn();
                    }
                }

                /**
                 * If the user writes manually, including paste operation, the projectFilePath, then it's very important to reset variables
                 * , otherwise it could lead to unpredictable validation errors.
                 *
                 * When project file is set manually, we have 2 cases:
                 * case 1:
                 *  if this file is a blend file, then:
                 *      selectedProjectFileExtension - will be set here
                 *      runnableBlendFileName -
                 * case 2:
                 *  if the file is an archive with blend files:
                 *      selectedProjectFileExtension - will be set here
                 *      runnableBlendFileName - should be selected a runnableBlendFileName from the content of archive
                 */
                private void updateSaveBtn() {

                    if (isUsedForProjectFile) {
                        resetVariables();
                    }

                    String path = MandatoryFileTextField.this.getText().trim();
                    File file = new File(path);
                    if (path.isEmpty() || !file.exists() || !file.canRead() || (isUsedForProjectFile && !isValidProjectFile(file))) {
                        saveButton.setEnabled(false);
                        valid = false;
                        setBorder(errorBorder);
                    } else {
                        valid = true;
                        setBorder(defaultBorder);
                        boolean enableSaveButton = jobNameTextField.isValid() && projectFileTextField.isValid() && outputFolderTextField.isValid()
                                && firstFrameTextField.areValidBothFrameFields() && commandLineScriptTextField.isValid();
                        saveButton.setEnabled(enableSaveButton);

                    }
                }

                private boolean isValidProjectFile(File file) {
                    return file.isFile() && hasValidExtension(file);
                }

                /**
                 * Avoid here to select a runnableBlendFileName for
                 * @param projectFile
                 * @return
                 */
                private boolean hasValidExtension(File projectFile) {

                    String fileName = projectFile.getName();
                    FileExtensions fileExtension = FileExtensions.getByFileName(fileName);
                    runnableBlendFileName = null;
                    selectedProjectFileExtension = fileExtension;

                    switch (fileExtension) {

                        case BLEND:
                            runnableBlendFileName = fileName;
                    }

                    return fileExtension != null;
                }
            });
        }

        /**
         * Lets avoid validation mechanism, because of
         * java.lang.IllegalStateException: Attempt to mutate in notification
         *
         * @param text
         */
        public void presetText(String text) {
            isPresetText = true;
            setText(text);
            isPresetText = false;
            valid = true;
        }

        @Override
        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public File getFile() {
            return new File(getText());
        }
    }

    private class MandatoryIntegerTextField extends JTextField {

        /**
         * is valid like Integer value
         * , but condition for this flag doesn't include comparing between firstFrameTextField and lastFrameTextField !
         */
        private boolean valid;
        private boolean isFirst;
        private Integer value;
        private Border defaultBorder = getBorder();

        public MandatoryIntegerTextField(String toolTipText, boolean isFirst) {
            this(toolTipText);
            this.isFirst = isFirst;
        }

        public MandatoryIntegerTextField(String toolTipText) {

            setToolTipText(toolTipText);

            getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateSaveBtn();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateSaveBtn();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateSaveBtn();
                }

                private void updateSaveBtn() {
                    valid = false;
                    String inputValue = MandatoryIntegerTextField.this.getText().trim();
                    if (!inputValue.isEmpty()) {
                        try {
                            value = Integer.valueOf(inputValue);
                            if (value >= 1) {
                                valid = true;
                            } else {
                                valid = false;
                                value = null;
                            }
                        } catch (NumberFormatException e) {
                        }
                    }

                    if (valid) {
                        setBorder(defaultBorder);
                        boolean areValidBothFrameFields = areValidBothFrameFields();
                        setBordersToBothFields(areValidBothFrameFields ? defaultBorder : errorBorder);

                        boolean enableSaveButton = jobNameTextField.isValid() && projectFileTextField.isValid() && outputFolderTextField.isValid()
                                && areValidBothFrameFields && commandLineScriptTextField.isValid();
                        saveButton.setEnabled(enableSaveButton);
                    } else {
                        setBordersToBothFields(errorBorder);
                        saveButton.setEnabled(false);
                    }
                }

                private void setBordersToBothFields(Border border) {
                    firstFrameTextField.setBorder(border);
                    lastFrameTextField.setBorder(border);
                }
            });
        }

        /**
         * Could be called from both fields, because will give exactly the same result
         *
         * @return
         */
        public boolean areValidBothFrameFields() {

            return firstFrameTextField.isValid() && lastFrameTextField.isValid() && (
                    isFirst ? value <= lastFrameTextField.getValue()
                            : value >= firstFrameTextField.getValue()
            );
        }

        @Override
        public boolean isValid() {
            return valid;
        }

        private Integer getValue() {
            return value;
        }
    }

    private class RequiredJLabel extends JLabel {

        /**
         * For required label
         *
         * @param text
         */
        public RequiredJLabel(String text, String requiredText) {
            super(String.format("<html><div align=right width=90px>%s: <font color='red'>%s</font></div></html>", text, requiredText));
        }

        /**
         * For non required label
         *
         * @param text
         */
        public RequiredJLabel(String text) {
            super(String.format("<html><div align=right width=90px>%s: </div></html>", text));
        }
    }
}
