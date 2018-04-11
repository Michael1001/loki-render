package net.whn.loki.common;

import net.whn.loki.commandLine.CommandLineHelper;
import net.whn.loki.master.FileExtensions;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a task of one or more tasks within a job
 */
public class Task implements ICommon, Serializable, Cloneable {

    private static long taskIDCounter = 0;
    final private long id;

    private final JobType jobType;
    private final int frame;
    private final long jobId;
    /**
     * Could be either blend file or archive with many blend files.
     * For example like:
     * C:\Users\angel\Desktop\ManyBlendFiles.zip
     * or
     * C:\Users\angel\Desktop\BMW27GE.blend
     *
     * And its value reflecting master value, it means original file location chosen by the user in master
     */
    private final File projectFile;
    /**
     * like "ManyBlendFiles.zip" or "BMW27GE.blend"
     */
    private final String projectFileName;
    /**
     * Used for case when project file is an archive.
     * So for the archive: "ManyBlendFiles.zip" -> value will be "ManyBlendFiles"
     */
    private final String projectFileNameWithoutExtension;
    private final String runnableBlenderFileName;
    private final long projectFileSize;
    private final String outputFolderName;
    private final String outputFilePrefix;
    private final boolean autoFileTransfer;
    private boolean tileRender;
    private int tile = -1;
    private int tilesForFrame = -1;
    private TileBorder tileBorder;
    private volatile TaskStatus status = TaskStatus.READY;
    private long gruntId = -1;
    private String gruntName = "";

    //output
    private String[] taskCL;
    private String stdout;
    private String errout;
    private String taskTime;
    private long outputFileSize = 1;
    private List<RenderedFileAttribute> renderedFileAttributes;
    private boolean isKeepingFoldersStructure;
    private boolean isEnabledAutoRunScripts;
    private boolean isEnabledCommandLineScripts;
    private List<String> commandLineScriptArgs = new ArrayList<>();
    private int tileMultiplier;
    private final FileExtensions selectedProjectFileExtension;

    /**
     * Create special, for case: when we use "-F TIFF", it will generate a file with extension ".tif"
     * So this field, take it in count.
     */
    private String generatedOutputFileExtension;

    /**
     * By default: ".png"
     * Or take indicated format from the command line args
     */
    private String imageFormatPython;

    /**
     * Used for simple rendering, without tiling
     * @param jobType
     * @param frame
     * @param jobId
     * @param outputFolderName
     * @param outputFilePrefix
     * @param projectFile
     * @param autoFileTransfer
     * @param isKeepingFoldersStructure
     * @param isEnabledAutoRunScripts
     * @param isEnabledCommandLineScripts
     * @param commandLineScriptArgs
     */
    public Task(JobType jobType,
                int frame,
                long jobId,
                String outputFolderName,
                String outputFilePrefix,
                File projectFile,
                String projectFileNameWithoutExtension,
                FileExtensions selectedProjectFileExtension,
                boolean autoFileTransfer,
                boolean isKeepingFoldersStructure,
                boolean isEnabledAutoRunScripts,
                boolean isEnabledCommandLineScripts,
                List<String> commandLineScriptArgs,
                String runnableBlenderFileName) {

        id = taskIDCounter++;

        this.jobType = jobType;
        this.frame = frame;
        this.jobId = jobId;
        this.projectFileName = projectFile.getName();
        this.projectFileSize = projectFile.length();
        this.outputFolderName = outputFolderName;
        this.outputFilePrefix = outputFilePrefix;
        this.projectFile = projectFile;
        this.projectFileNameWithoutExtension = projectFileNameWithoutExtension;
        this.autoFileTransfer = autoFileTransfer;

        renderedFileAttributes = new ArrayList<>();
        this.isKeepingFoldersStructure = isKeepingFoldersStructure;
        this.isEnabledAutoRunScripts = isEnabledAutoRunScripts;
        this.isEnabledCommandLineScripts = isEnabledCommandLineScripts;
        this.commandLineScriptArgs = commandLineScriptArgs;
        determineOutputFileExtension();
        this.selectedProjectFileExtension = selectedProjectFileExtension;
        this.runnableBlenderFileName = runnableBlenderFileName;
    }

    /**
     * Is used for tiling rendering
     * @param jobType
     * @param frame
     * @param jobId
     * @param outputFolderName
     * @param outputFilePrefix
     * @param projectFile
     * @param autoFileTransfer
     * @param tileRender
     * @param tile
     * @param tilesForFrame
     * @param tileBorder
     * @param isKeepingFoldersStructure
     * @param isEnabledAutoRunScripts
     * @param isEnabledCommandLineScripts
     * @param commandLineScriptArgs
     * @param tileMultiplier
     */
    public Task(JobType jobType,
                int frame,
                long jobId,
                String outputFolderName,
                String outputFilePrefix,
                File projectFile,
                String projectFileNameWithoutExtension,
                FileExtensions selectedProjectFileExtension,
                boolean autoFileTransfer,
                boolean tileRender,
                int tile,
                int tilesForFrame,
                TileBorder tileBorder,
                boolean isKeepingFoldersStructure,
                boolean isEnabledAutoRunScripts,
                boolean isEnabledCommandLineScripts,
                List<String> commandLineScriptArgs,
                int tileMultiplier,
                String runnableBlenderFileName) {

        this(jobType,
                frame,
                jobId,
                outputFolderName,
                outputFilePrefix,
                projectFile,
                projectFileNameWithoutExtension,
                selectedProjectFileExtension,
                autoFileTransfer,
                isKeepingFoldersStructure,
                isEnabledAutoRunScripts,
                isEnabledCommandLineScripts,
                commandLineScriptArgs,
                runnableBlenderFileName);

        this.tileRender = tileRender;
        this.tile = tile;
        this.tilesForFrame = tilesForFrame;
        this.tileBorder = tileBorder;
        this.tileMultiplier = tileMultiplier;
    }

    private void determineOutputFileExtension() {
        if (isEnabledCommandLineScripts) {
            int i = commandLineScriptArgs.indexOf("-F");
            if (i != -1) {
                String blenderImageFormat = commandLineScriptArgs.get(++i);
                switch (blenderImageFormat) {
                    case "TIFF":
                        imageFormatPython ="TIFF";
                        generatedOutputFileExtension = ".tif";
                        break;
                    case "TGA":
                        imageFormatPython ="TARGA";
                        generatedOutputFileExtension = ".tga";
                        break;
                    case "RAWTGA":
                        imageFormatPython ="TARGA_RAW";
                        generatedOutputFileExtension = ".tga";
                        break;
                    case "JPEG":
                        imageFormatPython ="JPEG";
                        generatedOutputFileExtension = ".jpg";
                        break;
                    case "EXR":
                        imageFormatPython ="OPEN_EXR";
                        generatedOutputFileExtension = ".exr";
                        break;
                    default:
                        imageFormatPython ="PNG";
                        generatedOutputFileExtension = ".png";
                }
            }
        } else {
            // when tile rendering functional will not be seen in UI, this should be rethought!
            generatedOutputFileExtension = ".png";
            imageFormatPython = "PNG";
            commandLineScriptArgs.addAll(Arrays.asList("-F", "PNG"));
        }
    }

    @Override
    public Task clone() throws CloneNotSupportedException {
        return (Task) super.clone();
    }

    /**
     * called by master once on shutdown to get current idcounter
     * @return the current job id counter
     */
    public static long getTaskIDCounter() {
        return taskIDCounter;
    }

    /**
     * called by master once on startup if we're loading from a cfg file
     * @param jCounter
     */
    public static void setTaskIDCounter(long jCounter) {
        taskIDCounter = jCounter;
    }

    public JobType getJobType() {
        return jobType;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus newStatus) {
        status = newStatus;
    }

    public int getFrame() {
        return frame;
    }

    public int getTile() {
        return tile;
    }

    public int getTilesPerFrame() {
        return tilesForFrame;
    }

    public boolean isTile() {
        return tileRender;
    }

    public TileBorder getTileBorder() {
        return tileBorder;
    }

    public long getId() {
        return id;
    }

    public long getJobId() {
        return jobId;
    }

    public long getGruntId() {
        return gruntId;
    }

    public void setGruntId(long gID) {
        gruntId = gID;
    }

    public void setGruntName(String gName) {
        gruntName = gName;
    }

    public String getGruntName() {
        return gruntName;
    }

    public String getProjectFileName() {
        return projectFileName;
    }

    public long getProjectFileSize() {
        return projectFileSize;
    }

    public String[] getTaskCL() {
        return taskCL;
    }

    public String getStdout() {
        return stdout;
    }

    public String getErrOut() {
        return errout;
    }

    public String getTaskTime() {
        return taskTime;
    }

    public long getOutputFileSize() {
        return outputFileSize;
    }

    public String getOutputFolderName() {
        return outputFolderName;
    }

    public String getOutputFilePrefix() {
        return outputFilePrefix;
    }
    
    public File getProjectFile() {
        return projectFile;
    }
    
    public boolean isAutoFileTranfer() {
        return autoFileTransfer;
    }
    
    public void setErrout(String eout) {
        errout = eout;
    }

    public void setInitialOutput(String[] tCL, String sOut, String eOut) {
        taskCL = tCL;
        stdout = sOut;
        errout = eOut;
    }

    public void determineStatus() {
        status = CommandLineHelper.determineTaskReturn(jobType, stdout, errout);
    }

    public void populateDoneInfo() {

        taskTime = CommandLineHelper.extractBlenderRenderTime(stdout);
        renderedFileAttributes.clear();

        for (File file : CommandLineHelper.getBlenderRenderedFiles(stdout)) {
            RenderedFileAttribute attribute = new RenderedFileAttribute();
            attribute.setFile(file);
            attribute.setFileLength(file.length());
            renderedFileAttributes.add(attribute);
        }
    }

    public List<RenderedFileAttribute> getRenderedFileAttributes() {
        return renderedFileAttributes;
    }

    public boolean isKeepingFoldersStructure() {
        return isKeepingFoldersStructure;
    }

    public boolean isEnabledAutoRunScripts() {
        return isEnabledAutoRunScripts;
    }

    public void setEnabledAutoRunScripts(boolean enabledAutoRunScripts) {
        isEnabledAutoRunScripts = enabledAutoRunScripts;
    }

    public boolean isEnabledCommandLineScripts() {
        return isEnabledCommandLineScripts;
    }

    public void setEnabledCommandLineScripts(boolean enabledCommandLineScripts) {
        isEnabledCommandLineScripts = enabledCommandLineScripts;
    }

    public List<String> getCommandLineScriptArgs() {
        return commandLineScriptArgs;
    }

    public void setCommandLineScriptArgs(List<String> commandLineScriptArgs) {
        this.commandLineScriptArgs = commandLineScriptArgs;
    }

    public String getGeneratedOutputFileExtension() {
        return generatedOutputFileExtension;
    }

    public void setGeneratedOutputFileExtension(String generatedOutputFileExtension) {
        this.generatedOutputFileExtension = generatedOutputFileExtension;
    }

    public int getTileMultiplier() {
        return tileMultiplier;
    }

    public void setTileMultiplier(int tileMultiplier) {
        this.tileMultiplier = tileMultiplier;
    }

    public String getImageFormatPython() {
        return imageFormatPython;
    }

    public void setImageFormatPython(String imageFormatPython) {
        this.imageFormatPython = imageFormatPython;
    }

    public String getGeneratedOutputFileExtensionWithoutDot() {
        return generatedOutputFileExtension.substring(1, generatedOutputFileExtension.length());
    }

    public FileExtensions getSelectedProjectFileExtension() {
        return selectedProjectFileExtension;
    }

    public String getRunnableBlenderFileName() {
        return runnableBlenderFileName;
    }

    public String getProjectFileNameWithoutExtension() {
        return projectFileNameWithoutExtension;
    }
}
