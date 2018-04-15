package net.whn.loki.common;

import net.whn.loki.commandLine.CommandLineHelper;
import net.whn.loki.common.ICommon.LokiRole;
import net.whn.loki.common.configs.Config;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreferencesForm extends LokiForm {

    private static final Logger log = Logger.getLogger(PreferencesForm.class.toString());
    private final Config config;
    private JButton blenderExeFileChooserButton;
    private JButton cancelButton;
    private JButton fileHelpButton;
    private JButton saveButton;
    private ButtonGroup fileHandlingButtonGroup;
    private ButtonGroup masterAddressButtonGroup;
    private ButtonGroup lokiRoleButtonGroup;
    private JLabel announceIntervalCaption;
    private JLabel controlsInfoCaption;
    private JLabel cacheSizeLimitCaptionPrefix;
    private JLabel cacheSizeLimitCaptionSufix;
    private JLabel announceMulticastIPGroupCaption;
    private JLabel announcePortCaption;
    private JLabel acceptPortCaption;
    private JLabel currentCacheSizeCaption;
    private JLabel blenderCaption;
    private JPanel blenderPanel;
    private JTabbedPane tabbedPane;
    private JPanel projectFileCachePanel;
    private JPanel scriptPanel;
    private JPanel fileHandlingPanel;
    private JPanel generalPanel;
    private JPanel localGruntPanel;
    private JPanel localMasterPanel;
    private JPanel masterIPPanel;
    private JPanel networkPanel;
    private JPanel lokiRolePanel;
    private JRadioButton askMeRadioButton;
    private JRadioButton autoFileRadioButton;
    private JRadioButton autoIPRadioButton;
    private JRadioButton gruntRadioButton;
    private JRadioButton manualFileRadioButton;
    private JRadioButton manualIPRadioButton;
    private JRadioButton masterRadioButton;
    private JRadioButton masterAndGruntRadioButton;
    private JSpinner spinnerCacheSizeLimit;
    private JTextField acceptPortTextField;
    private JTextField announceInterval;
    private JTextField blenderExeTextField;
    private JLabel currentCacheSizeTextField;
    private JTextField manualMasterIPTextField;
    private JTextField announceMulticastIP;
    private JTextField announcePort;
    private JCheckBox enableAutoRunScriptsCheckBox;
    private JCheckBox enableCommandLineScriptsCheckBox;

    private static final String IP_ADDRESS_PATTERN = getIpAddressPattern();

    public PreferencesForm(Config config) {

        this.config = config;
        initComponents();
        addWindowSettings();
        addSettings();
    }

    private void addSettings() {
        //general
        updateCacheValues();

        LokiRole lokiRole = config.getLokiRole();
        switch (lokiRole) {

            case ASK:
                askMeRadioButton.setSelected(true);
                break;
            case GRUNT:
                gruntRadioButton.setSelected(true);
                break;
            case MASTER:
                masterRadioButton.setSelected(true);
                break;
            case MASTER_GRUNT:
                masterAndGruntRadioButton.setSelected(true);
                break;
            default:
                log.severe("unknown role: " + lokiRole);
        }

        if (config.getAutoFileHandling()) {
            autoFileRadioButton.setSelected(true);
        } else {
            manualFileRadioButton.setSelected(true);
            projectFileCachePanel.setEnabled(false);
        }


        //grunt
        blenderExeTextField.setText(config.getBlenderBin());
        if (!config.isAutoDiscoverMaster()) {
            manualIPRadioButton.setSelected(true);
            manualMasterIPTextField.setText(config.getMasterIp().getHostAddress());
            manualMasterIPTextField.setEnabled(true);
        }

        //master
        announceMulticastIP.setText(config.getMulticastAddress().getHostAddress());
        announcePort.setText(Integer.toString(config.getGruntMulticastPort()));
        announceInterval.setText(Integer.toString(config.getAnnounceInterval()));
        acceptPortTextField.setText(Integer.toString(config.getConnectPort()));
    }

    public void updateCacheValues() {
        spinnerCacheSizeLimit.setValue(config.getCacheSizeLimitMB());
        currentCacheSizeTextField.setText(config.getCacheSizeStr());
    }

    public static boolean isValidIP(final String ip) {
        Pattern pattern = Pattern.compile(IP_ADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

    private void addWindowSettings() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Preferences");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                setVisible(false);
            }
        });
    }

    private void initComponents() {
        buildMainLayout();
        pack();
    }

    private void buildMainLayout() {
        GroupLayout mainLayout = new GroupLayout(getContentPane());
        getContentPane().setLayout(mainLayout);
        mainLayout.setHorizontalGroup(
                mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(buildControlsInfoCaption())
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(buildCancelButton(), GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(buildSaveButton())
                                .addContainerGap()
                        ).addGroup(mainLayout.createSequentialGroup()
                        .addComponent(buildTabbedPane(), GroupLayout.PREFERRED_SIZE, 620, GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)
                )
        );

        mainLayout.linkSize(SwingConstants.HORIZONTAL, cancelButton, saveButton);
        mainLayout.setVerticalGroup(
                mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(tabbedPane)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(mainLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(saveButton)
                                        .addComponent(cancelButton)
                                        .addComponent(controlsInfoCaption)
                                ).addContainerGap()
                        )
        );
    }

    private JTabbedPane buildTabbedPane() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("general", buildGeneralGroupLayout());
        tabbedPane.addTab("local grunt", buildLocalGruntGroupLayout());
        tabbedPane.addTab("local master", buildLocalMasterGroupLayout());
        return tabbedPane;
    }

    private JButton buildSaveButton() {
        saveButton = new JButton("Save");
        saveButton.addActionListener((event) -> save());
        return saveButton;
    }

    private JButton buildCancelButton() {
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener((event) -> setVisible(false));
        return cancelButton;
    }

    private JLabel buildControlsInfoCaption() {
        controlsInfoCaption = new JLabel();
        controlsInfoCaption.setFont(new Font("Ubuntu", 1, 15));
        controlsInfoCaption.setText("Restart Loki after 'Save' for all settings to take effect.");
        return controlsInfoCaption;
    }

    private JPanel buildLocalMasterGroupLayout() {

        GroupLayout localMasterGroupLayout = new GroupLayout(localMasterPanel = new JPanel());
        localMasterPanel.setLayout(localMasterGroupLayout);
        localMasterGroupLayout.setHorizontalGroup(
                localMasterGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(localMasterGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(buildNetworkPanelGroupLayout(), GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap()
                        )
        );
        localMasterGroupLayout.setVerticalGroup(
                localMasterGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(localMasterGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(networkPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(201, Short.MAX_VALUE)
                        )
        );
        return localMasterPanel;
    }

    private JPanel buildNetworkPanelGroupLayout() {

        buildAnnounceMulticastIPFields();
        buildAnnouncePortFields();
        buildAnnounceIntervalFields();
        buildAcceptPortFields();

        GroupLayout networkPanelGroupLayout = new GroupLayout(buildNetworkPanel());
        networkPanel.setLayout(networkPanelGroupLayout);
        networkPanelGroupLayout.setHorizontalGroup(
                networkPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(networkPanelGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(networkPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(announceMulticastIPGroupCaption, GroupLayout.Alignment.TRAILING)
                                        .addComponent(announcePortCaption, GroupLayout.Alignment.TRAILING)
                                        .addComponent(announceIntervalCaption, GroupLayout.Alignment.TRAILING)
                                        .addComponent(acceptPortCaption, GroupLayout.Alignment.TRAILING))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(networkPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(announceMulticastIP, GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
                                        .addGroup(networkPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                                .addComponent(acceptPortTextField, GroupLayout.Alignment.LEADING)
                                                .addComponent(announceInterval, GroupLayout.Alignment.LEADING)
                                                .addComponent(announcePort, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                                        )
                                )
                                .addContainerGap())
        );
        networkPanelGroupLayout.setVerticalGroup(
                networkPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(networkPanelGroupLayout.createSequentialGroup()
                                .addGroup(networkPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(announceMulticastIPGroupCaption)
                                        .addComponent(announceMulticastIP, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                ).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(networkPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(announcePortCaption)
                                        .addComponent(announcePort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                ).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(networkPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(networkPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(announceIntervalCaption)
                                        .addComponent(announceInterval, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                ).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(networkPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(acceptPortTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(acceptPortCaption)
                                ).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )
        );
        return networkPanel;
    }

    private void buildAcceptPortFields() {
        acceptPortCaption = new JLabel("Accept port:");

        acceptPortTextField = new JTextField();
        acceptPortTextField.setToolTipText("The port grunts use to establish a connection with the master.");
        acceptPortTextField.setEnabled(false);
    }

    private void buildAnnounceIntervalFields() {
        announceIntervalCaption = new JLabel("Announce interval (ms):");

        announceInterval = new JTextField();
        announceInterval.setToolTipText("Time interval between master announcements.");
        announceInterval.setEnabled(false);
    }

    private void buildAnnouncePortFields() {
        announcePortCaption = new JLabel("Announce port:");

        announcePort = new JTextField();
        announcePort.setToolTipText("This is the port used in combination with the multicast address.");
        announcePort.setEnabled(false);
    }

    private void buildAnnounceMulticastIPFields() {
        announceMulticastIPGroupCaption = new JLabel("Announce multicast group:");

        announceMulticastIP = new JTextField();
        announceMulticastIP.setToolTipText("This is the multicast address that the master uses to announce itself on the network. Grunts listen on this address to find the master.");
        announceMulticastIP.setEnabled(false);
    }

    private JPanel buildNetworkPanel() {
        networkPanel = new JPanel();
        networkPanel.setBorder(buildTitledBorder("Network settings"));
        networkPanel.setToolTipText("these networks settings cannot currently be changed");
        return networkPanel;
    }

    private JPanel buildLocalGruntGroupLayout() {

        GroupLayout localGruntGroupLayout = new GroupLayout(localGruntPanel = new JPanel());
        localGruntPanel.setLayout(localGruntGroupLayout);
        localGruntGroupLayout.setHorizontalGroup(
                localGruntGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, localGruntGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(localGruntGroupLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(buildMasterIPGroupLayout(), GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(buildBlenderGroupLayout(), GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                ).addContainerGap()
                        )
        );
        localGruntGroupLayout.setVerticalGroup(
                localGruntGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(localGruntGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(blenderPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(masterIPPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(92, Short.MAX_VALUE)
                        )
        );

        return localGruntPanel;
    }

    private JPanel buildMasterIPGroupLayout() {

        masterIPPanel = new JPanel();
        masterIPPanel.setBorder(buildTitledBorder("Master IP address"));
        masterIPPanel.setToolTipText("restart loki grunt for this setting to take effect");
        masterIPPanel.getAccessibleContext().setAccessibleName("Master  IP address");
        masterIPPanel.getAccessibleContext().setAccessibleDescription("");

        manualMasterIPTextField = new JTextField();
        manualMasterIPTextField.setEnabled(false);

        masterAddressButtonGroup = new ButtonGroup();

        autoIPRadioButton = new JRadioButton("automatic discovery of address", true);
        autoIPRadioButton.addActionListener(e -> manualMasterIPTextField.setEnabled(false));
        masterAddressButtonGroup.add(autoIPRadioButton);

        manualIPRadioButton = new JRadioButton("manually specify address");
        manualIPRadioButton.addActionListener(e -> manualMasterIPTextField.setEnabled(true));
        masterAddressButtonGroup.add(manualIPRadioButton);

        GroupLayout masterIPGroupLayout = new GroupLayout(masterIPPanel);
        masterIPPanel.setLayout(masterIPGroupLayout);
        masterIPGroupLayout.setHorizontalGroup(
                masterIPGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(masterIPGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(masterIPGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(autoIPRadioButton)
                                        .addComponent(manualIPRadioButton)
                                        .addComponent(manualMasterIPTextField, GroupLayout.PREFERRED_SIZE, 141, GroupLayout.PREFERRED_SIZE)
                                ).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )
        );
        masterIPGroupLayout.setVerticalGroup(
                masterIPGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(masterIPGroupLayout.createSequentialGroup()
                                .addComponent(autoIPRadioButton)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(manualIPRadioButton)
                                .addGap(18, 18, 18)
                                .addComponent(manualMasterIPTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(48, Short.MAX_VALUE)
                        )
        );
        return masterIPPanel;
    }

    private JPanel buildBlenderGroupLayout() {

        GroupLayout blenderGroupLayout = new GroupLayout(buildBlenderPanel());
        blenderPanel.setLayout(blenderGroupLayout);
        blenderGroupLayout.setHorizontalGroup(
                blenderGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(blenderGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(blenderGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(buildBlenderExeTextField(), GroupLayout.DEFAULT_SIZE, 552, Short.MAX_VALUE)
                                        .addGroup(blenderGroupLayout.createSequentialGroup()
                                                .addGroup(blenderGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(blenderCaption = new JLabel("Blender executable path:"))
                                                        .addComponent(buildBlenderExeFileChooserButton(), GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE)
                                                ).addGap(0, 0, Short.MAX_VALUE)
                                        )
                                ).addContainerGap()
                        )
        );
        blenderGroupLayout.setVerticalGroup(
                blenderGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(blenderGroupLayout.createSequentialGroup()
                                .addComponent(blenderCaption)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(blenderExeTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(blenderExeFileChooserButton)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )
        );
        return blenderPanel;
    }

    private JPanel buildBlenderPanel() {
        blenderPanel = new JPanel();
        blenderPanel.setBorder(buildTitledBorder("Blender"));
        return blenderPanel;
    }

    private JTextField buildBlenderExeTextField() {
        blenderExeTextField = new JTextField();
        blenderExeTextField.setToolTipText("loki needs to know the blender executable when it starts as a grunt");
        return blenderExeTextField;
    }

    private JButton buildBlenderExeFileChooserButton() {
        blenderExeFileChooserButton = new JButton("Browse");
        blenderExeFileChooserButton.addActionListener((event) -> chooseBlenderExe());
        return blenderExeFileChooserButton;
    }

    private TitledBorder buildTitledBorder(String blender) {
        return BorderFactory.createTitledBorder(null, blender, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("DejaVu Sans", 1, 13));
    }

    private JPanel buildGeneralGroupLayout() {

        buildLokiRoleGroupLayout();
        buildFileHandlingGroupLayout();
        buildProjectFileCacheGroupLayout();
        buildScriptPanelGroupLayout();

        GroupLayout generalPanelGroupLayout = new GroupLayout(buildGeneralPanel());
        generalPanel.setLayout(generalPanelGroupLayout);
        generalPanelGroupLayout.setHorizontalGroup(
                generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(generalPanelGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(projectFileCachePanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lokiRolePanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(fileHandlingPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(scriptPanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                ).addContainerGap()
                        )
        );
        generalPanelGroupLayout.setVerticalGroup(
                generalPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(generalPanelGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lokiRolePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fileHandlingPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(projectFileCachePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scriptPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )
        );

        return generalPanel;
    }

    private JPanel buildGeneralPanel() {
        generalPanel = new JPanel();
        generalPanel.setBorder(BorderFactory.createTitledBorder(""));
        return generalPanel;
    }

    private void buildProjectFileCacheGroupLayout() {

        cacheSizeLimitCaptionPrefix = new JLabel("Set a target limit of");
        cacheSizeLimitCaptionSufix = new JLabel("MB of space for the cache");

        currentCacheSizeCaption = new JLabel("Current cache size:");

        currentCacheSizeTextField = new JLabel("0 MB");
        currentCacheSizeTextField.setToolTipText("cache size will exceed the target limit if cached files are still associated with queued jobs.");

        spinnerCacheSizeLimit = new JSpinner(new SpinnerNumberModel(100, 50, 4000, 10));
        spinnerCacheSizeLimit.setToolTipText("Loki will limit the cache size to this value, unless all the cached files are still being used by the job queue, in which case the cache will be allowed to grow larger.");


        projectFileCachePanel = new JPanel();
        projectFileCachePanel.setBorder(buildTitledBorder("Project File Cache"));

        GroupLayout projectFileCacheGroupLayout = new GroupLayout(projectFileCachePanel);
        projectFileCachePanel.setLayout(projectFileCacheGroupLayout);
        projectFileCacheGroupLayout.setHorizontalGroup(
                projectFileCacheGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(projectFileCacheGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(projectFileCacheGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(projectFileCacheGroupLayout.createSequentialGroup()
                                                .addComponent(cacheSizeLimitCaptionPrefix)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(spinnerCacheSizeLimit, GroupLayout.PREFERRED_SIZE, 62, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cacheSizeLimitCaptionSufix)
                                        )
                                        .addGroup(projectFileCacheGroupLayout.createSequentialGroup()
                                                .addComponent(currentCacheSizeCaption)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(currentCacheSizeTextField)
                                        )
                                ).addContainerGap(162, Short.MAX_VALUE))
        );
        projectFileCacheGroupLayout.setVerticalGroup(
                projectFileCacheGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(projectFileCacheGroupLayout.createSequentialGroup()
                                .addGroup(projectFileCacheGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cacheSizeLimitCaptionPrefix)
                                        .addComponent(spinnerCacheSizeLimit, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cacheSizeLimitCaptionSufix)
                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(projectFileCacheGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(currentCacheSizeCaption)
                                        .addComponent(currentCacheSizeTextField)
                                ).addContainerGap(13, Short.MAX_VALUE)
                        )
        );
    }

    private void buildFileHandlingGroupLayout() {

        buildFileHandlingButtonGroup();

        GroupLayout fileHandlingGroupLayout = new GroupLayout(buildFileHandlingPanel());
        fileHandlingPanel.setLayout(fileHandlingGroupLayout);
        fileHandlingGroupLayout.setHorizontalGroup(
                fileHandlingGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(fileHandlingGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(fileHandlingGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(fileHandlingGroupLayout.createSequentialGroup()
                                                .addComponent(autoFileRadioButton)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(buildFileHelpButton(), GroupLayout.PREFERRED_SIZE, 68, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(fileHandlingGroupLayout.createSequentialGroup()
                                                .addComponent(manualFileRadioButton)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        fileHandlingGroupLayout.setVerticalGroup(
                fileHandlingGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(fileHandlingGroupLayout.createSequentialGroup()
                                .addGroup(fileHandlingGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(autoFileRadioButton)
                                        .addComponent(fileHelpButton))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(manualFileRadioButton)
                                .addContainerGap(18, Short.MAX_VALUE))
        );
    }

    private JButton buildFileHelpButton() {
        fileHelpButton = new JButton();
        fileHelpButton.setText("Help");
        fileHelpButton.addActionListener((event) -> btnFileHelpActionPerformed());
        return fileHelpButton;
    }

    private void buildFileHandlingButtonGroup() {

        fileHandlingButtonGroup = new ButtonGroup();
        fileHandlingButtonGroup.add(buildAutoFileRadioButton());
        fileHandlingButtonGroup.add(buildManualFileRadioButton());
    }

    private JRadioButton buildManualFileRadioButton() {
        manualFileRadioButton = new JRadioButton("Disable");
        manualFileRadioButton.setToolTipText("You'll need to setup a network share in this mode. Typically used for large projects.");
        manualFileRadioButton.addActionListener(e -> setManualFileHandling());
        return manualFileRadioButton;
    }

    private JRadioButton buildAutoFileRadioButton() {
        autoFileRadioButton = new JRadioButton("Enable", true);
        autoFileRadioButton.setToolTipText("Loki automatically transfers and caches files between nodes as needed. \nIn most cases you want this.");
        autoFileRadioButton.addActionListener(e -> setAutoFileHandling());
        return autoFileRadioButton;
    }

    private JPanel buildFileHandlingPanel() {
        fileHandlingPanel = new JPanel();
        fileHandlingPanel.setBorder(buildTitledBorder("Automatic File Transfer and Caching"));
        return fileHandlingPanel;
    }

    private JPanel builLokiRolePanel() {
        lokiRolePanel = new JPanel();
        lokiRolePanel.setBorder(buildTitledBorder("Role"));
        return lokiRolePanel;
    }

    private void buildLokiRoleGroupLayout() {

        JLabel selectLokiRoleCaption = new JLabel("Select Loki Render's role on startup:");
        buildLokiRoleButtonGroup();

        GroupLayout lokiRoleGroupLayout = new GroupLayout(builLokiRolePanel());
        lokiRolePanel.setLayout(lokiRoleGroupLayout);
        lokiRoleGroupLayout.setHorizontalGroup(
                lokiRoleGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(lokiRoleGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(lokiRoleGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(selectLokiRoleCaption)
                                        .addGroup(lokiRoleGroupLayout.createSequentialGroup()
                                                .addGap(12, 12, 12)
                                                .addGroup(lokiRoleGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(gruntRadioButton)
                                                        .addComponent(askMeRadioButton)
                                                        .addComponent(masterRadioButton)
                                                        .addComponent(masterAndGruntRadioButton)
                                                )
                                        )
                                ).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        lokiRoleGroupLayout.setVerticalGroup(
                lokiRoleGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(lokiRoleGroupLayout.createSequentialGroup()
                                .addComponent(selectLokiRoleCaption)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(askMeRadioButton)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(gruntRadioButton)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(masterRadioButton)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(masterAndGruntRadioButton)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )
        );
    }

    private void buildLokiRoleButtonGroup() {
        lokiRoleButtonGroup = new ButtonGroup();
        lokiRoleButtonGroup.add(askMeRadioButton = new JRadioButton("Ask me", true));
        lokiRoleButtonGroup.add(gruntRadioButton = new JRadioButton("Grunt"));
        lokiRoleButtonGroup.add(masterRadioButton = new JRadioButton("Master"));
        lokiRoleButtonGroup.add(masterAndGruntRadioButton = new JRadioButton("Master and Grunt"));
    }

    private void buildScriptPanelGroupLayout() {

        enableAutoRunScriptsCheckBox = new JCheckBox("Enable Auto-Run Scripts");
        enableAutoRunScriptsCheckBox.setSelected(config.isEnabledAutoRunScripts());

        enableCommandLineScriptsCheckBox = new JCheckBox("Enable command line scripts");
        enableCommandLineScriptsCheckBox.setSelected(config.isEnabledCommandLineScripts());

        scriptPanel = new JPanel();
        scriptPanel.setBorder(buildTitledBorder("Scripts"));

        GroupLayout scriptPanelGroupLayout = new GroupLayout(scriptPanel);
        scriptPanel.setLayout(scriptPanelGroupLayout);
        scriptPanelGroupLayout.setHorizontalGroup(
                scriptPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(scriptPanelGroupLayout
                                .createSequentialGroup()
                                .addContainerGap()
                                .addGroup(scriptPanelGroupLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(enableAutoRunScriptsCheckBox)
                                        .addComponent(enableCommandLineScriptsCheckBox)
                                )
                        )
        );
        scriptPanelGroupLayout.setVerticalGroup(scriptPanelGroupLayout
                .createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(scriptPanelGroupLayout
                        .createSequentialGroup()
                        .addComponent(enableAutoRunScriptsCheckBox)
                        .addComponent(enableCommandLineScriptsCheckBox)
                )
        );
    }

    private void save() {

        boolean valid = true;
        String mistakeStr = null;

        switch (Main.myRole) {

            case GRUNT:
            case GRUNT_COMMAND_LINE:
            case MASTER_GRUNT:
                if (!CommandLineHelper.isValidBlenderExe(blenderExeTextField.getText())) {
                    valid = false;
                    mistakeStr = "'" + blenderExeTextField.getText() + "' is not a valid Blender \n" + "executable.";
                }
        }

        config.setLokiRole(askMeRadioButton.isSelected() ? LokiRole.ASK
                : gruntRadioButton.isSelected() ? LokiRole.GRUNT
                : masterRadioButton.isSelected() ? LokiRole.MASTER
                : LokiRole.MASTER_GRUNT
        );

        config.setCacheSizeLimitMB((Integer) spinnerCacheSizeLimit.getValue());

        //Grunt
        if (autoIPRadioButton.isSelected()) {
            config.setAutoDiscoverMaster(true);
        }
        if (manualIPRadioButton.isSelected()) {
            if (isValidIP(manualMasterIPTextField.getText())) {
                try {
                    InetAddress testy =
                            InetAddress.getByName(manualMasterIPTextField.getText());
                    config.setMasterIp(testy);
                    config.setAutoDiscoverMaster(false);
                } catch (UnknownHostException uhex) {
                    valid = false;
                    mistakeStr = "Please enter a valid Master IP address.";
                    autoIPRadioButton.setSelected(true);
                    manualMasterIPTextField.setEnabled(false);
                }
            } else {
                valid = false;
                mistakeStr = "Please enter a valid Master IP address.";
                autoIPRadioButton.setSelected(true);
                manualMasterIPTextField.setEnabled(false);
            }
        }

        if (valid) {
            config.setBlenderBin(blenderExeTextField.getText());
            config.setEnabledAutoRunScripts(enableAutoRunScriptsCheckBox.isSelected());
            config.setEnabledCommandLineScripts(enableCommandLineScriptsCheckBox.isSelected());

            setVisible(false);
        } else {
            JOptionPane.showMessageDialog(null, mistakeStr, "Notice", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void chooseBlenderExe() {

        String blenderBinStr = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Please select the Blender executable");
        if (fileChooser.showDialog(null, "Select") == JFileChooser.APPROVE_OPTION) {
            blenderBinStr = fileChooser.getSelectedFile().getPath();

            if (CommandLineHelper.isValidBlenderExe(blenderBinStr)) {
                blenderExeTextField.setText(blenderBinStr);
            } else {
                String msg = "'" + blenderBinStr + "' is not a valid Blender \n" + "executable.";
                JOptionPane.showMessageDialog(null, msg, "Notice", JOptionPane.WARNING_MESSAGE);

                log.info("not a valid blender executable: " + blenderBinStr);
            }
        }
    }

    private void setAutoFileHandling() {
        config.setAutoFileHandling(true);
        projectFileCachePanel.setEnabled(true);
        cacheSizeLimitCaptionPrefix.setEnabled(true);
        spinnerCacheSizeLimit.setEnabled(true);
        cacheSizeLimitCaptionSufix.setEnabled(true);
        currentCacheSizeCaption.setEnabled(true);
        currentCacheSizeTextField.setEnabled(true);
    }

    private void setManualFileHandling() {
        config.setAutoFileHandling(false);
        projectFileCachePanel.setEnabled(false);
        cacheSizeLimitCaptionPrefix.setEnabled(false);
        spinnerCacheSizeLimit.setEnabled(false);
        cacheSizeLimitCaptionSufix.setEnabled(false);
        currentCacheSizeCaption.setEnabled(false);
        currentCacheSizeTextField.setEnabled(false);
    }

    private void btnFileHelpActionPerformed() {
        String msg =
                "When enabled, Loki will automatically transfer\n" +
                        "the blend file to grunts, and then the resulting\n" +
                        "renders sent back to the master.  In this mode you should\n" +
                        "always pack your textures and project files into your blend\n" +
                        "file.\n" +
                        "Additionally, files are cached on the grunts so that\n" +
                        "frequently used files are just pulled from the cache\n" +
                        "instead of across the network every time they're needed.\n" +
                        "\n" +
                        "You'll usually want this enabled, but in certain cases\n" +
                        "this is not ideal, for example if you have a large\n" +
                        "project with many files, then loki will spend a lot\n" +
                        "of time sending files, caching them, etc, and\n" +
                        "you'll also end up with copies of all the project\n" +
                        "files in each grunt's cache.\n\n" +
                        "In such a case select 'Disabled' and setup a network\n" +
                        "share that all computers can access, (both read AND write!)\n" +
                        "and place project files on this share. Then point Loki's\n" +
                        "project paths all to this one central place.\n" +
                        "\n" +
                        "Such a share can be setup in many ways: Windows share,\n" +
                        "NFS, or SSHFS are a few examples. More advanced distributed\n" +
                        "file systems such as GPFS or Lustre could also be used.\n" +
                        "\n" +
                        "IMPORTANT! - When set to 'Disabled' you must make certain\n" +
                        "that all computers running loki have the EXACT SAME path\n" +
                        "to the project files! For example if you're running Windows\n" +
                        "workstations, then your project path might be:\n" +
                        "X:\\projects\\blender\\projectx\\\n" +
                        "or if all your computers are running linux, then maybe:\n" +
                        "/var/projects/blender/projectx/\n";

        JOptionPane.showMessageDialog(null, msg, "About File Management", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String getIpAddressPattern() {
        return "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    }
}
