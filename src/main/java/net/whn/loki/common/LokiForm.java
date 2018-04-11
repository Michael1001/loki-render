package net.whn.loki.common;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class LokiForm extends JFrame {

    public LokiForm() {
        initComponents();
        setupIcon();
    }

    private void initComponents() {

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }

    private void setupIcon() {
        URL url = Main.class.getResource("/images/lokiIcon.png");
        setIconImage(Toolkit.getDefaultToolkit().createImage(url));
    }
}
