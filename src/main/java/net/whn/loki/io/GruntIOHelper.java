package net.whn.loki.io;

import net.whn.loki.common.ProgressUpdate;
import net.whn.loki.common.Task;
import net.whn.loki.common.TileBorder;
import net.whn.loki.common.configs.Config;
import net.whn.loki.grunt.GruntEQCaller;
import net.whn.loki.grunt.GruntForm;
import net.whn.loki.grunt.GruntR;
import net.whn.loki.network.GruntStreamSocket;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Logger;

public class GruntIOHelper extends IOHelper {

    private static final Logger log = Logger.getLogger(GruntIOHelper.class.toString());

    public static String generateHumanReadableFileSize(long size) {
        DecimalFormat fmt = new DecimalFormat("#0.0");
        final long bytesPerKB = 1024;
        final long bytesPerMB = 1048576;
        final long bytesPerGB = 1073741824;
        String txt;
        if (size < bytesPerKB) {
            txt = Long.toString(size) + " bytes";
        } else if (size < bytesPerMB) {
            double result = (double) size / (double) bytesPerKB;
            txt = fmt.format(result) + " KB";
        } else if (size < bytesPerGB) {
            double result = (double) size / (double) bytesPerMB;
            txt = fmt.format(result) + " MB";
        } else if (size >= bytesPerGB) {
            double result = (double) size / (double) bytesPerGB;
            txt = fmt.format(result) + " GB";
        } else {
            log.severe("freaky!");
            txt = null;
        }
        return "(" + txt + ")";
    }

    /**
     * receives file from broker via network, and adds to cache
     */
    public static void receiveFileFromBroker(String projectFileName,
                                                long projectFileSize,
                                                Config config,
                                                GruntForm gruntForm,
                                                GruntStreamSocket gruntStreamSocket
    ) throws IOException {

        File projectFile = new File(GruntR.CACHE_FOLDER, projectFileName);
        gruntStreamSocket.receiveFileFromBroker(projectFile, projectFileSize, gruntForm);
        addProjectFileToCache(projectFile, config);
    }

    public static long countAllFilesSize(List<File> files) {

        long result = 0L;
        for (File file : files) {
            result += file.length();
        }
        return result;
    }

    /**
     * @param gruntForm
     * @param files
     * @param gruntStreamSocket
     * @param isTileRender
     * @param allFilesSize
     */
    public static void sendOutputFilesToBroker(GruntForm gruntForm, List<File> files, GruntStreamSocket gruntStreamSocket, boolean isTileRender, long allFilesSize) {

        long totalRead = 0;
        long remaining = allFilesSize;

        for (File file : files) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int amountRead;
            boolean isSocketOutFlushed = false;

            try (InputStream inputStream = new FileInputStream(file)) {
                while ((amountRead = inputStream.read(buffer)) > -1) {
                    gruntStreamSocket.sendFileChunk(buffer, amountRead);
                    remaining -= amountRead;
                    totalRead += amountRead;
                    Integer percentValue = (int) (totalRead * 100 / allFilesSize);
                    gruntStreamSocket.updatePercentQueue(percentValue);
                    if (gruntForm != null)
                        GruntEQCaller.invokeGruntUpdatePBar(gruntForm, new ProgressUpdate(allFilesSize, remaining));
                }
                gruntStreamSocket.flushSockOut();
                isSocketOutFlushed = true;

            } catch (IOException e) {
                log.severe("file to send not found: " + e.getMessage());
            }
            if (isSocketOutFlushed && gruntStreamSocket.wasFileReceived()) {
                try {
                    if (isTileRender) {
                        FileUtils.cleanDirectory(file.getParentFile());
                    } else {
                        Files.delete(file.toPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try { // need to reset, to let be used socket further for Header
            gruntStreamSocket.getObjectOut().reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
        gruntStreamSocket.finishFillingPercentQueue();
    }

    /**
     * Is used just for tiling render
     * @param gruntRenderFolder
     * @param task
     * @return
     * @throws IOException
     */
    public static File createBlenderPhytonScript(File gruntRenderFolder, Task task) throws IOException {

        File script = new File(gruntRenderFolder, "setupTileBorder.py");

        if (script.isFile() && !script.delete()) {
            log.warning("unable to delete renderTile.py");
        }
        TileBorder tileBorder = task.getTileBorder();

        PrintWriter writer = new PrintWriter(new FileWriter(script));
        writer.println("import bpy");
        writer.println("import os");
        writer.println("left = " + Float.toString(tileBorder.getLeft()));
        writer.println("right = " + Float.toString(tileBorder.getRight()));
        writer.println("bottom = " + Float.toString(tileBorder.getBottom()));
        writer.println("top = " + Float.toString(tileBorder.getTop()));
        writer.println("scene  = bpy.context.scene");
        writer.println("render = scene.render");
        writer.println("render.use_border = True");
        writer.println("render.use_crop_to_border = True");

        writer.println(getRenderFileFormat(task));

        if (!task.getImageFormatPython().equals("JPEG")) {
            writer.println("render.image_settings.color_mode = 'RGBA'"); // aici posibil sa mai tre ceva
        }

        writer.println("render.use_file_extension = True");
        writer.println("render.border_max_x = right");
        writer.println("render.border_min_x = left");
        writer.println("render.border_max_y = top");
        writer.println("render.border_min_y = bottom");
        writer.println(getPhytonRenderFilePath());
        writer.println("scene.frame_start = " + task.getFrame());
        writer.println("scene.frame_end = " + task.getFrame());
        // if it's not used this line, the scene uses their original parameters...
        writer.println("bpy.ops.render.render(animation=True)");
        writer.flush();
        writer.close();
        return script;
    }

    /**
     * @return path like: "render.filepath = os.path.dirname(bpy.data.filepath) + os.sep + '..' + os.sep + 'renderFolder' + os.sep"
     *
     * Goes from ".loki\grunt\fileCache\ManyBlendFiles\" folder where are located runnable file: BMW-TEST.blend
     * , to ".loki\grunt\renderFolder\", where is located created pyhton script setupTileBorder.py
     *
     * So each tiled rendered file will be saved on this path, at the grunt machine
     */
    private static String getPhytonRenderFilePath() {

        String phytonFileSeparator = " + os.sep";

        String s = new StringBuilder("render.filepath = os.path.dirname(bpy.data.filepath)")
                .append(phytonFileSeparator)
                .append(getPhytonFolder(".."))
                .append(phytonFileSeparator)
                .append(getPhytonFolder(GruntR.RENDER_FOLDER_NAME))
                .append(phytonFileSeparator)
                .toString();
        return s;
    }

    private static String getPhytonFolder(String folderName) {
        return " + \'" + folderName + "\'";
    }

    /**
     * replacer for hard coded: "render.image_settings.file_format = 'PNG'"
     * wok for TIFF and PNG
     * @param task
     * @return
     */
    private static String getRenderFileFormat(Task task) {
        return String.format("render.image_settings.file_format = '%s'", task.getImageFormatPython());
    }
}
