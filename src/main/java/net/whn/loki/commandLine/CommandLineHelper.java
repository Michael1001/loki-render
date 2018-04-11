package net.whn.loki.commandLine;

import net.whn.loki.common.ICommon;
import net.whn.loki.common.Task;
import net.whn.loki.common.configs.Config;
import net.whn.loki.grunt.GruntR;
import net.whn.loki.io.GruntIOHelper;
import net.whn.loki.io.IOHelper;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class CommandLineHelper implements ICommon {

    private static final Logger log = Logger.getLogger(CommandLineHelper.class.toString());

    /**
     * generates command line based on type and values in task, auto file transfer
     *
     * @param task
     * @return command line to pass to shell, or null if unknown type
     */
    public static String[] generateFileTaskCommandLine(String blenderBin, Task task)  throws IOException {
        String[] taskCL = null;
        JobType type = task.getJobType();
        if (type == JobType.BLENDER) {
            taskCL = generateBlenderCommandLine(blenderBin, task);
        } else {
            log.severe("received unknown job type!");
        }
        return taskCL;
    }

    public static TaskStatus determineTaskReturn(JobType type, String stdout, String errout) {
        if (type == JobType.BLENDER) {
            return blender_determineTaskReturn(stdout);
        } else {
            log.severe("received unknown job type!");
            return null;
        }
    }

    /**
     *
     * @param stdout
     * @return - an unique set of files.
     *
     * When using tiling, with command line args like "-F TIFF", or any other format, blender will produce 2 files:
     * first a png file, then convert it to indicated format.
     * So in the ".loki/tmp", we will have 2 files: for ex: 0001.png and 0001.tiff
     * In the case of tiling, we need to take only one file, of indicated format, from the STDOUT
     * Further it will be sent back to the master
     *
     * If we will use tiling, but without command line args, then blender will produce again 2 files: but the second will override first
     * , because it has the same name, and file extension ".png", and finally in the ".tmp" folder of the Grunt machine, will be present just one file.
     */
    public static Set<File> getBlenderRenderedFiles(String stdout) {

        Set<File> files = new LinkedHashSet<>();
        String savedPrefix = "Saved: ";
        for (String line : stdout.split("\\n")) {
            if (line.startsWith(savedPrefix)) {
                String fileName = line.split(savedPrefix)[1].trim().replaceAll("'", "");
                files.add(new File(fileName));
            }
        }
        return files;
    }

    /**
     *
     * @param stdout
     * @return a value like: "44:17.54"
     */
    public static String extractBlenderRenderTime(String stdout) {
        try {
            return stdout.split("\\n Time: ")[1].split(" ")[0].trim();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean couldDetermineBlenderBin(Config config) {

        String blenderBinString = config.getBlenderBin();
        if (blenderBinString == null) {
            blenderBinString = "blender";
        }

        if (!isValidBlenderExeQuietly(blenderBinString)) { //no good, ask user to find it

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Please select the Blender executable");

            while (true) {
                if (fileChooser.showDialog(null, "Select") == JFileChooser.APPROVE_OPTION) {

                    blenderBinString = fileChooser.getSelectedFile().getPath();

                    if (isValidBlenderExe(blenderBinString)) {
                        break;
                    } else {
                        String msg = new StringBuilder("Loki can't validate'")
                                .append(System.lineSeparator())
                                .append(blenderBinString)
                                .append(System.lineSeparator())
                                .append("as a Blender executable. Use it anyway?")
                                .toString();

                        int result = JOptionPane.showConfirmDialog(null, msg, "Valid executable?", JOptionPane.YES_NO_OPTION);

                        log.info("can't validate blender executable: " + blenderBinString);
                        if (result == JFileChooser.APPROVE_OPTION)
                            break;
                    }
                } else {
                    log.info("loki didn't get a blender exe path; exiting.");
                    return false;
                }
            }
        }
        config.setBlenderBin(blenderBinString);

        return true;
    }

    public static boolean isValidBlenderExeQuietly(String blenderBinString) {

        String[] commandLine = {blenderBinString, "-v"};

        return new ProcessHelper(commandLine).runProcessQuitely()[0].contains("Blender");
    }

    public static boolean isValidBlenderExe(String blenderBinString) {

        String[] commandLine = {blenderBinString, "-v"};
        ProcessHelper processHelper = new ProcessHelper(commandLine);

        String[] result = processHelper.runProcess();

        if (result[0].contains("Blender")) {
            return true;
        } else {
            log.info("not a valid blender executable: " + blenderBinString);
            return false;
        }
    }

    private static String[] generateBlenderCommandLine(String blenderBin, Task task) throws IOException {

        File blendFile = null;
        File gruntRenderFolder = GruntR.RENDER_FOLDER;

        File projectFile = task.getProjectFile();
        String projectFileNameWithoutExtension = task.getProjectFileNameWithoutExtension();

        if (task.isAutoFileTranfer()) {
            switch (task.getSelectedProjectFileExtension()) {

                case ZIP:
                    File currentJobFolder = new File(GruntR.CACHE_FOLDER, projectFileNameWithoutExtension);
                    File gruntZipFile = new File(GruntR.CACHE_FOLDER, projectFile.getName());
                    if (!currentJobFolder.exists()) {
                        IOHelper.unzipFile(gruntZipFile, currentJobFolder);
                        gruntZipFile.delete();
                    }
                    blendFile = new File(currentJobFolder, task.getRunnableBlenderFileName());
                    break;
//                case TAR:
//                    break;
//                case GZ:
//                    break;
                case BLEND:
                    blendFile = new File(GruntR.CACHE_FOLDER, projectFile.getName());
                    break;
            }
        } else {    //manual
            throw new RuntimeException("Manual file transfer under work!");
        }

        List<String> blenderCommandLine = new ArrayList<>();
        blenderCommandLine.add(blenderBin);
        blenderCommandLine.add("-noaudio");
        blenderCommandLine.add("-nojoystick");
        blenderCommandLine.add("-b");
        blenderCommandLine.add(blendFile.getCanonicalPath());

        if (blendFile.canRead()) {
            if (task.isTile()) {
                File script = GruntIOHelper.createBlenderPhytonScript(gruntRenderFolder, task);
                    
                blenderCommandLine.add("-P");
                blenderCommandLine.add(script.getCanonicalPath());

            } else { //render the entire frame
                     //example 'blender -b file.blend -o render_# -f 1

                blenderCommandLine.add("-o");
                blenderCommandLine.add(task.isAutoFileTranfer()
                        ? gruntRenderFolder.getCanonicalPath() + File.separator
                        : gruntRenderFolder.getCanonicalPath() + File.separator + task.getOutputFilePrefix() //manual
                );
            }

            if (task.isEnabledAutoRunScripts()) {
                blenderCommandLine.add("--enable-autoexec");
            }

            if (task.isEnabledCommandLineScripts()) {
                blenderCommandLine.addAll(task.getCommandLineScriptArgs());
            }

            blenderCommandLine.add("-f");
            blenderCommandLine.add(String.valueOf(task.getFrame()));
        } else {
            log.severe("problems generating blender commandLine: " +
                    blendFile.getAbsolutePath() + " " +
                    gruntRenderFolder.getAbsolutePath());
        }

        return blenderCommandLine.toArray(new String[]{});
    }

    private static TaskStatus blender_determineTaskReturn(String stdout) {
        return stdout.contains("Saved:") ? TaskStatus.DONE : TaskStatus.FAILED;
    }
}
