package net.whn.loki.common;

import javax.swing.*;
import java.awt.*;

public class EQCaller implements ICommon {

    /**
     * displays message dialog
     * @param frame
     * @param title
     * @param msg
     * @param messageType
     */
    public static void showMessageDialog(final JFrame frame,
                                         final String title,
                                         final String msg,
                                         final int messageType) {

        EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(frame, msg, title, messageType));
    }

}
