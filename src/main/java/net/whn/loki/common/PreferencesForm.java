package net.whn.loki.common;

import net.whn.loki.commandLine.CommandLineHelper;
import net.whn.loki.common.ICommon.LokiRole;
import net.whn.loki.common.configs.Config;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreferencesForm extends LokiForm {

    private static final Logger log = Logger.getLogger(PreferencesForm.class.toString());
    private final Config config;
    private JButton btnBrowseForBlenderBin;
    private JButton btnCancel;
    private JButton btnFileHelp;
    private JButton saveButton;
    private ButtonGroup btngrpFileHandling;
    private ButtonGroup btngrpMasterAddress;
    private ButtonGroup btngrpRole;
    private JLabel jLabel1;
    private JLabel jLabel10;
    private JLabel jLabel11;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JLabel jLabel4;
    private JLabel jLabel5;
    private JLabel jLabel6;
    private JLabel jLabel7;
    private JLabel jLabel9;
    private JPanel jPanel1;
    private JTabbedPane jTabbedPane1;
    private JPanel cachePanel;
    private JPanel scriptPanel;
    private JPanel pnlFileHandling;
    private JPanel pnlGeneral;
    private JPanel pnlGrunt;
    private JPanel pnlMaster;
    private JPanel pnlMasterIp;
    private JPanel pnlNetwork;
    private JPanel pnlRole;
    private JRadioButton askMeRadioButton;
    private JRadioButton autoFileRadioButton;
    private JRadioButton rbtnAutoIP;
    private JRadioButton gruntRadioButton;
    private JRadioButton manualFileRadioButton;
    private JRadioButton manualIPRadioButton;
    private JRadioButton masterRadioButton;
    private JRadioButton masterAndGruntRadioButton;
    private JSpinner spinnerCacheSizeLimit;
    private JTextField acceptPortTextField;
    private JTextField announceIntervalTextField;
    private JTextField blenderBinTextField;
    private JLabel currentCacheSizeTextField;
    private JTextField masterManualIpTextField;
    private JTextField multicastAddressTextField;
    private JTextField multicastPortTextField;
    private JCheckBox enableAutoRunScriptsCheckBox;
    private JCheckBox enableCommandLineScriptsCheckBox;

    private static final String IP_ADDRESS_PATTERN = getIpAddressPattern();

    public PreferencesForm(Config config) {
        this.config = config;
        initComponents();

        //general
        updateCacheValues();

        LokiRole lokiRole = this.config.getLokiRole();
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

        if (this.config.getAutoFileHandling()) {
            autoFileRadioButton.setSelected(true);
        } else {
            manualFileRadioButton.setSelected(true);
            cachePanel.setEnabled(false);
        }


        //grunt
        blenderBinTextField.setText(this.config.getBlenderBin());
        if (!this.config.isAutoDiscoverMaster()) {
            manualIPRadioButton.setSelected(true);
            masterManualIpTextField.setText(this.config.getMasterIp().getHostAddress());
            masterManualIpTextField.setEnabled(true);
        }

        //master
        multicastAddressTextField.setText(this.config.getMulticastAddress().getHostAddress());
        multicastPortTextField.setText(Integer.toString(this.config.getGruntMulticastPort()));
        announceIntervalTextField.setText(Integer.toString(this.config.getAnnounceInterval()));
        acceptPortTextField.setText(Integer.toString(this.config.getConnectPort()));
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

    @SuppressWarnings("unchecked")
    private void initComponents() {

        btngrpRole = new ButtonGroup();
        btngrpMasterAddress = new ButtonGroup();
        btngrpFileHandling = new ButtonGroup();
        jTabbedPane1 = new JTabbedPane();
        pnlGeneral = new JPanel();
        pnlRole = new JPanel();
        jLabel1 = new JLabel();
        gruntRadioButton = new JRadioButton();
        masterRadioButton = new JRadioButton();
        masterAndGruntRadioButton = new JRadioButton();
        askMeRadioButton = new JRadioButton();
        cachePanel = new JPanel();
        scriptPanel = new JPanel();

        jLabel2 = new JLabel();
        spinnerCacheSizeLimit = new JSpinner();
        jLabel3 = new JLabel();
        jLabel7 = new JLabel();
        currentCacheSizeTextField = new JLabel();
        pnlFileHandling = new JPanel();
        autoFileRadioButton = new JRadioButton();
        manualFileRadioButton = new JRadioButton();
        btnFileHelp = new JButton();
        pnlGrunt = new JPanel();
        jPanel1 = new JPanel();
        jLabel9 = new JLabel();
        blenderBinTextField = new JTextField();
        btnBrowseForBlenderBin = new JButton();
        pnlMasterIp = new JPanel();
        rbtnAutoIP = new JRadioButton();
        manualIPRadioButton = new JRadioButton();
        masterManualIpTextField = new JTextField();
        pnlMaster = new JPanel();
        pnlNetwork = new JPanel();
        jLabel4 = new JLabel();
        jLabel5 = new JLabel();
        jLabel6 = new JLabel();
        multicastAddressTextField = new JTextField();
        multicastPortTextField = new JTextField();
        acceptPortTextField = new JTextField();
        jLabel10 = new JLabel();
        announceIntervalTextField = new JTextField();
        btnCancel = new JButton();
        saveButton = new JButton();
        jLabel11 = new JLabel();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Preferences");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        pnlGeneral.setBorder(BorderFactory.createTitledBorder(""));

        pnlRole.setBorder(BorderFactory.createTitledBorder(null, "Role", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 13))); // NOI18N
        pnlRole.setToolTipText("");

        jLabel1.setText("Select Loki Render's role on startup:");

        btngrpRole.add(gruntRadioButton);
        gruntRadioButton.setText("Grunt");

        btngrpRole.add(masterRadioButton);
        masterRadioButton.setText("Master");

        btngrpRole.add(masterAndGruntRadioButton);
        masterAndGruntRadioButton.setText("Master and Grunt");

        btngrpRole.add(askMeRadioButton);
        askMeRadioButton.setSelected(true);
        askMeRadioButton.setText("Ask me");

        GroupLayout pnlRoleLayout = new GroupLayout(pnlRole);
        pnlRole.setLayout(pnlRoleLayout);
        pnlRoleLayout.setHorizontalGroup(
            pnlRoleLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlRoleLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlRoleLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(pnlRoleLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(pnlRoleLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(gruntRadioButton)
                            .addComponent(askMeRadioButton)
                            .addComponent(masterRadioButton)
                            .addComponent(masterAndGruntRadioButton))))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlRoleLayout.setVerticalGroup(
            pnlRoleLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlRoleLayout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(askMeRadioButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(gruntRadioButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(masterRadioButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(masterAndGruntRadioButton)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        cachePanel.setBorder(BorderFactory.createTitledBorder(null, "Project File Cache", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 13))); // NOI18N

        jLabel2.setText("Set a target limit of");

        spinnerCacheSizeLimit.setModel(new SpinnerNumberModel(100, 50, 4000, 10));
        spinnerCacheSizeLimit.setToolTipText("Loki will limit the cache size to this value, unless all the cached files are still being used by the job queue, in which case the cache will be allowed to grow larger.");

        jLabel3.setText("MB of space for the cache");

        jLabel7.setText("Current cache size:");

        currentCacheSizeTextField.setText("0 MB");
        currentCacheSizeTextField.setToolTipText("cache size will exceed the target limit if cached files are still associated with queued jobs.");

        GroupLayout pnlCacheLayout = new GroupLayout(cachePanel);
        cachePanel.setLayout(pnlCacheLayout);
        pnlCacheLayout.setHorizontalGroup(
            pnlCacheLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlCacheLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlCacheLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(pnlCacheLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerCacheSizeLimit, GroupLayout.PREFERRED_SIZE, 62, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3))
                    .addGroup(pnlCacheLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(currentCacheSizeTextField)))
                .addContainerGap(162, Short.MAX_VALUE))
        );
        pnlCacheLayout.setVerticalGroup(
            pnlCacheLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlCacheLayout.createSequentialGroup()
                .addGroup(pnlCacheLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(spinnerCacheSizeLimit, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlCacheLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(currentCacheSizeTextField))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        buildScriptPanel(pnlCacheLayout);

        pnlFileHandling.setBorder(BorderFactory.createTitledBorder(null, "Automatic File Transfer and Caching", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 13))); // NOI18N

        btngrpFileHandling.add(autoFileRadioButton);
        autoFileRadioButton.setSelected(true);
        autoFileRadioButton.setText("Enable");
        autoFileRadioButton.setToolTipText("Loki automatically transfers and caches files between nodes as needed. \nIn most cases you want this.");
        autoFileRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbtnAutoFileActionPerformed(evt);
            }
        });

        btngrpFileHandling.add(manualFileRadioButton);
        manualFileRadioButton.setText("Disable");
        manualFileRadioButton.setToolTipText("You'll need to setup a network share in this mode. Typically used for large projects.");
        manualFileRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbtnManualFileActionPerformed(evt);
            }
        });

        btnFileHelp.setText("Help");
        btnFileHelp.addActionListener((event) -> btnFileHelpActionPerformed());

        GroupLayout pnlFileHandlingLayout = new GroupLayout(pnlFileHandling);
        pnlFileHandling.setLayout(pnlFileHandlingLayout);
        pnlFileHandlingLayout.setHorizontalGroup(
            pnlFileHandlingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlFileHandlingLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlFileHandlingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(pnlFileHandlingLayout.createSequentialGroup()
                        .addComponent(autoFileRadioButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnFileHelp, GroupLayout.PREFERRED_SIZE, 68, GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlFileHandlingLayout.createSequentialGroup()
                        .addComponent(manualFileRadioButton)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pnlFileHandlingLayout.setVerticalGroup(
            pnlFileHandlingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlFileHandlingLayout.createSequentialGroup()
                .addGroup(pnlFileHandlingLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(autoFileRadioButton)
                    .addComponent(btnFileHelp))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(manualFileRadioButton)
                .addContainerGap(18, Short.MAX_VALUE))
        );

        GroupLayout pnlGeneralLayout = new GroupLayout(pnlGeneral);
        pnlGeneral.setLayout(pnlGeneralLayout);
        pnlGeneralLayout.setHorizontalGroup(
            pnlGeneralLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlGeneralLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlGeneralLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(cachePanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlRole, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlFileHandling, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(scriptPanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnlGeneralLayout.setVerticalGroup(
            pnlGeneralLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlGeneralLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlRole, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlFileHandling, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cachePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scriptPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            )
        );

        jTabbedPane1.addTab("general", pnlGeneral);

        jPanel1.setBorder(BorderFactory.createTitledBorder(null, "Blender", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 13))); // NOI18N

        jLabel9.setText("Blender executable path:");

        blenderBinTextField.setToolTipText("loki needs to know the blender executable when it starts as a grunt");

        btnBrowseForBlenderBin.setText("Browse");
        btnBrowseForBlenderBin.addActionListener((event) -> btnBrowseForBlenderBinActionPerformed());

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(blenderBinTextField, GroupLayout.DEFAULT_SIZE, 552, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9)
                            .addComponent(btnBrowseForBlenderBin, GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel9)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(blenderBinTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnBrowseForBlenderBin)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pnlMasterIp.setBorder(BorderFactory.createTitledBorder(null, "Master IP address", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 13))); // NOI18N
        pnlMasterIp.setToolTipText("restart loki grunt for this setting to take effect");

        btngrpMasterAddress.add(rbtnAutoIP);
        rbtnAutoIP.setSelected(true);
        rbtnAutoIP.setText("automatic discovery of address");
        rbtnAutoIP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbtnAutoIPActionPerformed(evt);
            }
        });

        btngrpMasterAddress.add(manualIPRadioButton);
        manualIPRadioButton.setText("manually specify address");
        manualIPRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbtnManualIPActionPerformed(evt);
            }
        });

        masterManualIpTextField.setEnabled(false);

        GroupLayout pnlMasterIpLayout = new GroupLayout(pnlMasterIp);
        pnlMasterIp.setLayout(pnlMasterIpLayout);
        pnlMasterIpLayout.setHorizontalGroup(
            pnlMasterIpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlMasterIpLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlMasterIpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(rbtnAutoIP)
                    .addComponent(manualIPRadioButton)
                    .addComponent(masterManualIpTextField, GroupLayout.PREFERRED_SIZE, 141, GroupLayout.PREFERRED_SIZE))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlMasterIpLayout.setVerticalGroup(
            pnlMasterIpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlMasterIpLayout.createSequentialGroup()
                .addComponent(rbtnAutoIP)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(manualIPRadioButton)
                .addGap(18, 18, 18)
                .addComponent(masterManualIpTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(48, Short.MAX_VALUE))
        );

        GroupLayout pnlGruntLayout = new GroupLayout(pnlGrunt);
        pnlGrunt.setLayout(pnlGruntLayout);
        pnlGruntLayout.setHorizontalGroup(
            pnlGruntLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, pnlGruntLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlGruntLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(pnlMasterIp, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnlGruntLayout.setVerticalGroup(
            pnlGruntLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlGruntLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlMasterIp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(92, Short.MAX_VALUE))
        );

        pnlMasterIp.getAccessibleContext().setAccessibleName("Master  IP address");
        pnlMasterIp.getAccessibleContext().setAccessibleDescription("");

        jTabbedPane1.addTab("local grunt", pnlGrunt);

        pnlNetwork.setBorder(BorderFactory.createTitledBorder(null, "Network settings", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 13))); // NOI18N
        pnlNetwork.setToolTipText("these networks settings cannot currently be changed");

        jLabel4.setText("Announce multicast group:");

        jLabel5.setText("Announce port:");

        jLabel6.setText("Accept port:");

        multicastAddressTextField.setToolTipText("This is the multicast address that the master uses to announce itself on the network. Grunts listen on this address to find the master.");
        multicastAddressTextField.setEnabled(false);

        multicastPortTextField.setToolTipText("This is the port used in combination with the multicast address.");
        multicastPortTextField.setEnabled(false);

        acceptPortTextField.setToolTipText("The port grunts use to establish a connection with the master.");
        acceptPortTextField.setEnabled(false);

        jLabel10.setText("Announce Interval (ms):");

        announceIntervalTextField.setToolTipText("Time interval between master announcements.");
        announceIntervalTextField.setEnabled(false);

        GroupLayout pnlNetworkLayout = new GroupLayout(pnlNetwork);
        pnlNetwork.setLayout(pnlNetworkLayout);
        pnlNetworkLayout.setHorizontalGroup(
            pnlNetworkLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlNetworkLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlNetworkLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5, GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel10, GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel6, GroupLayout.Alignment.TRAILING))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlNetworkLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(multicastAddressTextField, GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
                    .addGroup(pnlNetworkLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                        .addComponent(acceptPortTextField, GroupLayout.Alignment.LEADING)
                        .addComponent(announceIntervalTextField, GroupLayout.Alignment.LEADING)
                        .addComponent(multicastPortTextField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pnlNetworkLayout.setVerticalGroup(
            pnlNetworkLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlNetworkLayout.createSequentialGroup()
                .addGroup(pnlNetworkLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(multicastAddressTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlNetworkLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(multicastPortTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlNetworkLayout.createParallelGroup(GroupLayout.Alignment.BASELINE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlNetworkLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(announceIntervalTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlNetworkLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(acceptPortTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout pnlMasterLayout = new GroupLayout(pnlMaster);
        pnlMaster.setLayout(pnlMasterLayout);
        pnlMasterLayout.setHorizontalGroup(
            pnlMasterLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlMasterLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlNetwork, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        pnlMasterLayout.setVerticalGroup(
            pnlMasterLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(pnlMasterLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlNetwork, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(201, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("local master", pnlMaster);

        btnCancel.setText("Cancel");
        btnCancel.addActionListener((event) -> setVisible(false));

        saveButton.setText("Save");
        saveButton.addActionListener((event) -> btnSaveActionPerformed());

        jLabel11.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N
        jLabel11.setText("Restart Loki after 'Save' for all settings to take effect.");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(saveButton)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, GroupLayout.PREFERRED_SIZE, 620, GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        layout.linkSize(SwingConstants.HORIZONTAL, new java.awt.Component[] {btnCancel, saveButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(saveButton)
                    .addComponent(btnCancel)
                    .addComponent(jLabel11))
                .addContainerGap())
        );

        pack();
    }

    private void buildScriptPanel(GroupLayout pnlCacheLayout) {
        enableAutoRunScriptsCheckBox = new JCheckBox("Enable Auto-Run Scripts");
        enableAutoRunScriptsCheckBox.setSelected(config.isEnabledAutoRunScripts());

        enableCommandLineScriptsCheckBox = new JCheckBox("Enable command line scripts");
        enableCommandLineScriptsCheckBox.setSelected(config.isEnabledCommandLineScripts());

        GroupLayout scriptPanelLayout = new GroupLayout(scriptPanel);
        scriptPanel.setBorder(BorderFactory.createTitledBorder(null, "Scripts", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 13)));
        scriptPanel.setLayout(scriptPanelLayout);
        scriptPanelLayout.setHorizontalGroup(scriptPanelLayout
                .createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(pnlCacheLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(scriptPanelLayout
                                .createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(enableAutoRunScriptsCheckBox)
                                .addComponent(enableCommandLineScriptsCheckBox)
                        )
                )
        );
        scriptPanelLayout.setVerticalGroup(scriptPanelLayout
                .createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(scriptPanelLayout
                        .createSequentialGroup()
                        .addComponent(enableAutoRunScriptsCheckBox)
                        .addComponent(enableCommandLineScriptsCheckBox)
                )
        );
    }

    private void btnSaveActionPerformed() {

        boolean valid = true;
        String mistakeStr = null;

        switch (Main.myRole) {

            case GRUNT:
            case GRUNT_COMMAND_LINE:
            case MASTER_GRUNT:
                if (!CommandLineHelper.isValidBlenderExe(blenderBinTextField.getText())) {
                    valid = false;
                    mistakeStr = "'" + blenderBinTextField.getText() + "' is not a valid Blender \n" + "executable.";
                }
        }

        //General
        if (askMeRadioButton.isSelected()) {
            config.setLokiRole(LokiRole.ASK);
        } else if (gruntRadioButton.isSelected()) {
            config.setLokiRole(LokiRole.GRUNT);
        } else if (masterRadioButton.isSelected()) {
            config.setLokiRole(LokiRole.MASTER);
        } else if (masterAndGruntRadioButton.isSelected()) {
            config.setLokiRole(LokiRole.MASTER_GRUNT);
        } else {
            log.severe("unexpected state for role selection");
        }
        
        config.setCacheSizeLimitMB((Integer) spinnerCacheSizeLimit.getValue());

        //Grunt
        if (rbtnAutoIP.isSelected()) {
            config.setAutoDiscoverMaster(true);
        }
        if (manualIPRadioButton.isSelected()){
            if(isValidIP(masterManualIpTextField.getText())){
               try {
                   InetAddress testy = 
                           InetAddress.getByName(masterManualIpTextField.getText());
                    config.setMasterIp(testy);
                    config.setAutoDiscoverMaster(false);
                } catch (UnknownHostException uhex) {
                    valid = false;
                    mistakeStr = "Please enter a valid Master IP address.";
                    rbtnAutoIP.setSelected(true);
                    masterManualIpTextField.setEnabled(false);
                } 
            } else {
                valid = false;
                mistakeStr = "Please enter a valid Master IP address.";
                rbtnAutoIP.setSelected(true);
                masterManualIpTextField.setEnabled(false);
            }
        }

        if (valid) {
           config.setBlenderBin(blenderBinTextField.getText());
           config.setEnabledAutoRunScripts(enableAutoRunScriptsCheckBox.isSelected());
           config.setEnabledCommandLineScripts(enableCommandLineScriptsCheckBox.isSelected());
           
           setVisible(false);
        } else {
            JOptionPane.showMessageDialog(null, mistakeStr, "Notice", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void btnBrowseForBlenderBinActionPerformed() {
        String blenderBinStr = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Please select the Blender executable");
        if (fileChooser.showDialog(null, "Select") ==
                JFileChooser.APPROVE_OPTION) {
            blenderBinStr = fileChooser.getSelectedFile().getPath();

            if (CommandLineHelper.isValidBlenderExe(blenderBinStr)) {
                blenderBinTextField.setText(blenderBinStr);
            } else {
                String msg = "'" + blenderBinStr +
                        "' is not a valid Blender \n" + "executable.";
                JOptionPane.showMessageDialog(null, msg, "Notice",
                        JOptionPane.WARNING_MESSAGE);

                log.info("not a valid blender executable: " + blenderBinStr);
            }
        }
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        setVisible(false);
    }

    private void rbtnAutoIPActionPerformed(java.awt.event.ActionEvent evt) {
        masterManualIpTextField.setEnabled(false);
    }

    private void rbtnManualIPActionPerformed(java.awt.event.ActionEvent evt) {
        masterManualIpTextField.setEnabled(true);
    }

    private void rbtnAutoFileActionPerformed(java.awt.event.ActionEvent evt) {
        config.setAutoFileHandling(true);
        cachePanel.setEnabled(true);
        jLabel2.setEnabled(true);
        spinnerCacheSizeLimit.setEnabled(true);
        jLabel3.setEnabled(true);
        jLabel7.setEnabled(true);
        currentCacheSizeTextField.setEnabled(true);
    }

    private void rbtnManualFileActionPerformed(java.awt.event.ActionEvent evt) {
        config.setAutoFileHandling(false);
        cachePanel.setEnabled(false);
        jLabel2.setEnabled(false);
        spinnerCacheSizeLimit.setEnabled(false);
        jLabel3.setEnabled(false);
        jLabel7.setEnabled(false);
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
