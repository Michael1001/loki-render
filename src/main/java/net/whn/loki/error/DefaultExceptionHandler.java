package net.whn.loki.error;

import net.whn.loki.common.*;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

public class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger log = Logger.getLogger(DefaultExceptionHandler.class.toString());
    private final LokiForm form;

    public DefaultExceptionHandler(LokiForm lokiForm) {
        form = lokiForm;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {

        String message = new StringBuilder("Loki has encountered an error:")
                .append(System.lineSeparator())
                .append(throwable.toString())
                .append(System.lineSeparator())
                .append("Please view the log for details.")
                .append(System.lineSeparator())
                .append("Wisdom would dictate restarting Loki at this point.")
                .toString();

        String title = "Loki Render Error";

       JOptionPane.showMessageDialog(form, message, title, JOptionPane.ERROR_MESSAGE);
       
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
       
        log.warning("uncaught throwable: " + "\n" + sw.toString());
    }
}
