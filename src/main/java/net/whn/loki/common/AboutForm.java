package net.whn.loki.common;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class AboutForm extends LokiForm {

    private JPanel topPanel;
    private JPanel licensePanel;
    private JScrollPane bottomScrollPane;
    private Font textFont = new Font("Serif", Font.PLAIN, 16);

    public AboutForm() {
        initComponents();
    }

    private void initComponents() {

        buildTopPanel();
        buildBottomScrollPane();
        buildMainGroupLayout();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("About Loki Render");
        setMinimumSize(new Dimension(1100, 0));
        pack();
    }

    private JScrollPane buildBottomScrollPane() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(buildLicensePanel());

        return bottomScrollPane = new JScrollPane(panel);
    }

    private JPanel buildLicensePanel() {

        String lineSeparator = "<br>";

        String info = new StringBuilder("<html>")
                .append("Loki Render is free software: you can redistribute it and/or modify ")
                .append("it, under the terms of the GNU General Public License as published by ")
                .append("the Free Software Foundation")
                .append(lineSeparator)
                .append(", either version 3 of the License, or (at your option) any later version.")
                .append(lineSeparator)
                .append("Loki Render is distributed in the hope that it will be useful, ")
                .append("but WITHOUT ANY WARRANTY")
                .append(lineSeparator)
                .append(", without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.")
                .append(lineSeparator)
                .append("See the GNU General Public License for more details: http://www.gnu.org/licenses/")
                .append("</html>")
                .toString();

        JLabel licenseLabel = new JLabel(info);
        licenseLabel.setFont(textFont);



        licensePanel = new JPanel();
        TitledBorder titledBorder = BorderFactory.createTitledBorder("License");
        titledBorder.setTitleFont(new Font("Serif", Font.ROMAN_BASELINE, 18));
        licensePanel.setBorder(titledBorder);

        GroupLayout newFeaturesPanelGroupLayout = new GroupLayout(licensePanel);
        licensePanel.setLayout(newFeaturesPanelGroupLayout);
        newFeaturesPanelGroupLayout.setHorizontalGroup(
                newFeaturesPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(newFeaturesPanelGroupLayout.createSequentialGroup()
                                .addGap(40, 40, 40)
                                .addComponent(licenseLabel)
                        )
        );
        newFeaturesPanelGroupLayout.setVerticalGroup(
                newFeaturesPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(newFeaturesPanelGroupLayout.createSequentialGroup()
                                .addGroup(newFeaturesPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(licenseLabel)
                                )
                        )
        );
        return licensePanel;
    }

    private void buildTopPanel() {

        JLabel lokiLogo = new JLabel();
        lokiLogo.setIcon(new ImageIcon(getClass().getResource("/images/aboutLoki.png")));

        topPanel = new JPanel();
        GroupLayout bottomPanelGroupLayout = new GroupLayout(topPanel);
        topPanel.setLayout(bottomPanelGroupLayout);
        bottomPanelGroupLayout.setHorizontalGroup(
                bottomPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(bottomPanelGroupLayout.createSequentialGroup()
                                .addComponent(lokiLogo)
                                .addContainerGap()
                        )
        );
        bottomPanelGroupLayout.setVerticalGroup(
                bottomPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(bottomPanelGroupLayout.createSequentialGroup()
                                .addGroup(bottomPanelGroupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lokiLogo)
                                ).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }

    private void buildMainGroupLayout() {

        final int WIDTH = 800;

        JLabel logLabel = new JLabel("   Change log listed in the readme.txt file that comes with the project.");
        logLabel.setFont(textFont);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(topPanel, GroupLayout.DEFAULT_SIZE, WIDTH, Short.MAX_VALUE)
                                .addComponent(bottomScrollPane, GroupLayout.DEFAULT_SIZE, WIDTH, Short.MAX_VALUE)
                                .addComponent(logLabel, GroupLayout.DEFAULT_SIZE, WIDTH, Short.MAX_VALUE)
                        )
        );

        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(topPanel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(bottomScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(logLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                        )
        );
    }
}
