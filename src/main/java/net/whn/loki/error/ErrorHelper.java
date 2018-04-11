package net.whn.loki.error;

import net.whn.loki.common.LokiForm;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

public class ErrorHelper {

    /**
     * null lokiform is ok here; can handle it.
     * @param form
     * @param log
     * @param text
     * @param throwable
     */
    public static void outputToLogMsgAndKill(LokiForm form, boolean gruntcl, Logger log, String text, Throwable throwable) {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String msg = text + throwable.toString() + "\n" + sw.toString();
        log.severe(msg);
        
        if(!gruntcl) {
            JOptionPane.showMessageDialog(form, msg, "Fatal Error", JOptionPane.ERROR_MESSAGE);
        } else {
            System.out.println(msg);
        }
        System.exit(-1);
    }
}
