package net.whn.loki.master;

import net.whn.loki.common.GruntDetails;
import net.whn.loki.common.LokiForm;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class GruntDetailsForm extends LokiForm {

    private JButton okButton;
    private JLabel operatingSystemNameCaption;
    private JLabel usernameCaption;
    private JLabel userHomeCaption;
    private JLabel currentWorkingFolderCaption;
    private JLabel operatingSystemVersionCaption;
    private JLabel operatingSystemArchitectureCaption;
    private JLabel coresCaption;
    private JLabel totalPhysicalMemoryCaption;
    private JLabel totalSwapMemoryCaption;
    private JLabel cores;
    private JLabel userHome;
    private JLabel totalPhysicalMemory;
    private JLabel operatingSystemArchitecture;
    private JLabel operatingSystemName;
    private JLabel operatingSystemVersion;
    private JLabel totalSwapMemory;
    private JLabel username;
    private JLabel currentWorkingFolder;
    private JPanel environmentPanel;
    private JPanel operatingSystemPanel;
    private JPanel resourcesPanel;
    private JPanel gruntStatusPanel;
    private JLabel gruntStatusCaption;
    private JLabel gruntStatus;
    private GruntDetails details;
    private JLabel gruntErrorDetailsCaption;
    private JLabel gruntErrorDetails;

    public GruntDetailsForm(GruntDetails details) {
        this.details = details;
        initComponents();
        applySettingsFrom();
    }

    private void applySettingsFrom() {
        if (details != null) {
            setTitle("Grunt details for '" + details.getHostname() + "'");
            operatingSystemName.setText(details.getOsName());
            operatingSystemVersion.setText(details.getOsVersion());
            operatingSystemArchitecture.setText(details.getOsArchitecture());
            cores.setText(Integer.toString(details.getProcessors()));
            totalPhysicalMemory.setText(getReadableMemory(details.getTotalMemory()) + " GB");
            totalSwapMemory.setText(getReadableMemory(details.getTotalSwap()) + " GB");
            username.setText(details.getUserName());
            userHome.setText(details.getUserHome());
            currentWorkingFolder.setText(details.getCurrentWorkingFolder());

            gruntStatus.setText(details.getGruntStatus());
            String gruntErrorDetailsValue = details.getGruntErrorDetails();
            if (gruntErrorDetailsValue != null) {
                this.gruntErrorDetails.setText(gruntErrorDetailsValue);
            }
        }
    }

    private String getReadableMemory(long memory) {
        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        final long bytesPerGB = 1073741824;
        double result = (double) memory / (double) bytesPerGB;
        return decimalFormat.format(result);
    }

    private void initComponents() {

        buildGruntStatusPanel();
        buildOperatingSystemPanel();
        buildResourcesPanel();
        buildEnvironmentPanel();
        buildOkButton();
        buildMainGroupLayout();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Grunt Details");
        setMinimumSize(new Dimension(650, 0));
        pack();
    }

    private void buildMainGroupLayout() {

        GroupLayout mainGroupLayout = new GroupLayout(getContentPane());
        getContentPane().setLayout(mainGroupLayout);
        mainGroupLayout.setHorizontalGroup(
                mainGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(mainGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(gruntStatusPanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(operatingSystemPanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(resourcesPanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(environmentPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(okButton, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                )
                                .addContainerGap()
                        )
        );
        mainGroupLayout.setVerticalGroup(
                mainGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainGroupLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(gruntStatusPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(operatingSystemPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(resourcesPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(environmentPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(okButton)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )
        );
    }

    private JButton buildOkButton() {
        okButton = new JButton("OK");
        okButton.addActionListener((event) -> dispose());
        return okButton;
    }

    private void buildEnvironmentPanel() {

        environmentPanel = new JPanel();
        environmentPanel.setBorder(BorderFactory.createTitledBorder("Environment"));

        usernameCaption = new JLabel("username:");
        userHomeCaption = new JLabel("user home directory:");
        currentWorkingFolderCaption = new JLabel("current working directory:");

        username = new JLabel();
        userHome = new JLabel();
        currentWorkingFolder = new JLabel();

        GroupLayout environmentGroupLayout = new GroupLayout(environmentPanel);
        environmentPanel.setLayout(environmentGroupLayout);
        environmentGroupLayout.setHorizontalGroup(
                environmentGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(environmentGroupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(environmentGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(usernameCaption, GroupLayout.Alignment.TRAILING)
                                        .addComponent(userHomeCaption, GroupLayout.Alignment.TRAILING)
                                        .addComponent(currentWorkingFolderCaption, GroupLayout.Alignment.TRAILING))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(environmentGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(username)
                                        .addComponent(userHome)
                                        .addComponent(currentWorkingFolder))
                                .addContainerGap(288, Short.MAX_VALUE))
        );
        environmentGroupLayout.setVerticalGroup(
                environmentGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(environmentGroupLayout.createSequentialGroup()
                                .addGroup(environmentGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(usernameCaption)
                                        .addComponent(username))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(environmentGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(userHomeCaption)
                                        .addComponent(userHome))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(environmentGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(currentWorkingFolderCaption)
                                        .addComponent(currentWorkingFolder))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }

    private void buildResourcesPanel() {

        resourcesPanel = new JPanel();
        resourcesPanel.setBorder(BorderFactory.createTitledBorder("Resources"));

        coresCaption = new JLabel("cores:");
        totalPhysicalMemoryCaption = new JLabel("total physical memory:");
        totalSwapMemoryCaption = new JLabel("total swap memory:");

        cores = new JLabel();
        totalPhysicalMemory = new JLabel();
        totalSwapMemory = new JLabel();

        GroupLayout resourcesGroupLayout = new GroupLayout(resourcesPanel);
        resourcesPanel.setLayout(resourcesGroupLayout);
        resourcesGroupLayout.setHorizontalGroup(
                resourcesGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(resourcesGroupLayout.createSequentialGroup()
                                .addGap(27, 27, 27)
                                .addGroup(resourcesGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(coresCaption, GroupLayout.Alignment.TRAILING)
                                        .addComponent(totalPhysicalMemoryCaption, GroupLayout.Alignment.TRAILING)
                                        .addComponent(totalSwapMemoryCaption, GroupLayout.Alignment.TRAILING))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(resourcesGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(cores)
                                        .addComponent(totalPhysicalMemory)
                                        .addComponent(totalSwapMemory))
                                .addContainerGap(289, Short.MAX_VALUE))
        );
        resourcesGroupLayout.setVerticalGroup(
                resourcesGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(resourcesGroupLayout.createSequentialGroup()
                                .addGroup(resourcesGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(coresCaption)
                                        .addComponent(cores))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(resourcesGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(totalPhysicalMemoryCaption)
                                        .addComponent(totalPhysicalMemory))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(resourcesGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(totalSwapMemoryCaption)
                                        .addComponent(totalSwapMemory))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }

    private void buildOperatingSystemPanel() {

        operatingSystemPanel = new JPanel();
        operatingSystemPanel.setBorder(BorderFactory.createTitledBorder("Operating System"));

        operatingSystemNameCaption = new JLabel("name:");
        operatingSystemVersionCaption = new JLabel("version:");
        operatingSystemArchitectureCaption = new JLabel("architecture:");

        operatingSystemName = new JLabel();
        operatingSystemVersion = new JLabel();
        operatingSystemArchitecture = new JLabel();

        GroupLayout operatingSystemGroupLayout = new GroupLayout(operatingSystemPanel);
        operatingSystemPanel.setLayout(operatingSystemGroupLayout);
        operatingSystemGroupLayout.setHorizontalGroup(
                operatingSystemGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(operatingSystemGroupLayout.createSequentialGroup()
                                .addGap(73, 73, 73)
                                .addGroup(operatingSystemGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(operatingSystemArchitectureCaption, GroupLayout.Alignment.TRAILING)
                                        .addComponent(operatingSystemVersionCaption, GroupLayout.Alignment.TRAILING)
                                        .addComponent(operatingSystemNameCaption, GroupLayout.Alignment.TRAILING))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(operatingSystemGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(operatingSystemName)
                                        .addComponent(operatingSystemVersion)
                                        .addComponent(operatingSystemArchitecture))
                                .addContainerGap(288, Short.MAX_VALUE))
        );
        operatingSystemGroupLayout.setVerticalGroup(
                operatingSystemGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(operatingSystemGroupLayout.createSequentialGroup()
                                .addGroup(operatingSystemGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(operatingSystemNameCaption)
                                        .addComponent(operatingSystemName))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(operatingSystemGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(operatingSystemVersionCaption)
                                        .addComponent(operatingSystemVersion))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(operatingSystemGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(operatingSystemArchitectureCaption)
                                        .addComponent(operatingSystemArchitecture))
                                .addContainerGap(14, Short.MAX_VALUE))
        );
    }

    private void buildGruntStatusPanel() {

        gruntStatusPanel = new JPanel();
        gruntStatusPanel.setBorder(BorderFactory.createTitledBorder("General"));

        gruntStatusCaption = new JLabel("grunt status:");
        gruntStatus = new JLabel();

        gruntErrorDetailsCaption = new JLabel("details:");
        gruntErrorDetails = new JLabel();

        GroupLayout gruntStatusGroupLayout = new GroupLayout(gruntStatusPanel);
        gruntStatusPanel.setLayout(gruntStatusGroupLayout);

        GroupLayout.ParallelGroup parallelGroup1 = gruntStatusGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(gruntStatusCaption, GroupLayout.Alignment.TRAILING);

        GroupLayout.ParallelGroup parallelGroup2 = gruntStatusGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(gruntStatus);

        GroupLayout.SequentialGroup sequentialGroup = gruntStatusGroupLayout.createSequentialGroup()
                .addGroup(gruntStatusGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(gruntStatusCaption)
                        .addComponent(gruntStatus)
                );

        if (details.getGruntErrorDetails() != null) {

            parallelGroup1.addComponent(gruntErrorDetailsCaption, GroupLayout.Alignment.TRAILING);
            parallelGroup2.addComponent(gruntErrorDetails);

            sequentialGroup.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(gruntStatusGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(gruntErrorDetailsCaption)
                            .addComponent(gruntErrorDetails)
                    );
        }

        gruntStatusGroupLayout.setHorizontalGroup(
                gruntStatusGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(gruntStatusGroupLayout.createSequentialGroup()
                                .addGap(73, 73, 73)
                                .addGroup(parallelGroup1)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(parallelGroup2)
                                .addContainerGap(288, Short.MAX_VALUE)
                        )
        );

        gruntStatusGroupLayout.setVerticalGroup(
                gruntStatusGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(sequentialGroup.addContainerGap(14, Short.MAX_VALUE))
        );
    }
}
