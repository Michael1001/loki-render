package net.whn.loki.commandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

public class ProcessHelper {

    private static final Logger log = Logger.getLogger(ProcessHelper.class.toString());
    private final String[] taskCommandLine;
    private Process process;

    public ProcessHelper(String[] taskCommandLine) {
        this.taskCommandLine = taskCommandLine;
    }

    public String[] runProcessQuitely() {

        String result[] = {"", ""};
        try {
            process = new ProcessBuilder(taskCommandLine).start();

            Output stdout = new Output(process.getInputStream());
            FutureTask<String> stdTask = new FutureTask<>(stdout);
            Thread stdoutThread = new Thread(stdTask);
            stdoutThread.start();

            Output errout = new Output(process.getErrorStream());
            FutureTask<String> errTask = new FutureTask<>(errout);
            Thread erroutThread = new Thread(errTask);
            erroutThread.start();

            process.waitFor();

            result[0] = stdTask.get();
            result[1] = errTask.get();

            process.getOutputStream().close();

        } catch (Exception ex) {
        }

        return result;
    }

    public String[] runProcess() {
        String result[] = {"", ""};
        try {
            //process = Runtime.getRuntime().exec(taskCommandLine);
            process = new ProcessBuilder(taskCommandLine).start();

            Output stdout = new Output(process.getInputStream());
            FutureTask<String> stdTask = new FutureTask<String>(stdout);
            Thread stdoutThread = new Thread(stdTask);
            stdoutThread.start();

            Output errout = new Output(process.getErrorStream());
            FutureTask<String> errTask = new FutureTask<String>(errout);
            Thread erroutThread = new Thread(errTask);
            erroutThread.start();

            process.waitFor();

            result[0] = stdTask.get();
            result[1] = errTask.get();
            
            process.getOutputStream().close();

        } catch (IOException ex) {
            result[1] = "IOException: " + ex.getMessage();
            log.warning(ex.getMessage());

        } catch (InterruptedException ex) {
            result[1] = "InterruptedException: " + ex.getMessage();
            process.destroy();
            log.fine("finished interruptedException handling");
            
        } catch (ExecutionException ex) {
            result[1] = "ExecutionException: " + ex.getMessage();
            log.warning(ex.getMessage());
        }

        return result;
    }


    private class Output implements Callable<String> {

        private final InputStream is;

        Output(InputStream is) {
            this.is = is;
        }

        @Override
        public String call() {
            try {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
                    for (String line = null; (line = in.readLine()) != null;)
                        sb.append(line + "\n");
                }
                return sb.toString();

            } catch (IOException ex) {
                log.warning("unable to grab output from process: " + ex.getMessage());
            }
            return null;
        }
    }
}
