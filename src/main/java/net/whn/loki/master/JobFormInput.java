package net.whn.loki.master;

import java.util.List;

/**
 * An intermediate container to hold input from AddJobForm until we create an job object
 */
public class JobFormInput {

    private final String taskType;
    private final String jobName;
    private final String projectFilePath;
    private final String outputFolderName;
    private final String filePrefix;
    private final int firstTask;
    private final int lastTask;
    private final boolean isTileEnabled;
    private final int tileMultiplier;
    private final boolean isAutoFileTransfer;
    private final boolean isKeepingFoldersStructure;
    private final List<String> commandLineScriptArgs;
    private final FileExtensions selectedProjectFileExtension;
    /**
     * In the case if was selected a simple blend file, it will be located directly in GruntR.CACHE_FOLDER.
     * But if was selected an archive, in this case the runnable blend file could be located in some folder inside of this archive
     * , and in this case value of this variable could be something like:
     * "Rocks\Scenes\LOOKDEV\Props\Rocks\Link TEST.blend"
     * and this path will be relative to GruntR.CACHE_FOLDER.
     */
    private final String runnableBenderFileName;
    private final int renderigStep;
    private final boolean isEnabledAutoRunScripts;

    JobFormInput(String taskType,
                 String jobName,
                 String projectFilePath,
                 String outputFolderName,
                 String filePrefix,
                 int firstTask,
                 int lastTask,
                 boolean isTileEnabled,
                 int tileMultiplier,
                 boolean isAutoFileTransfer,
                 boolean isKeepingFoldersStructure,
                 List<String> commandLineScriptArgs,
                 FileExtensions selectedProjectFileExtension,
                 String runnableBenderFileName,
                 int renderigStep,
                 boolean isEnabledAutoRunScripts) {

        this.taskType = taskType;
        this.jobName = jobName;
        this.projectFilePath = projectFilePath;
        this.outputFolderName = outputFolderName;
        this.filePrefix = filePrefix;
        this.firstTask = firstTask;
        this.lastTask = lastTask;
        this.isTileEnabled = isTileEnabled;
        this.tileMultiplier = tileMultiplier;
        this.isAutoFileTransfer = isAutoFileTransfer;
        this.isKeepingFoldersStructure = isKeepingFoldersStructure;
        this.commandLineScriptArgs = commandLineScriptArgs;
        this.selectedProjectFileExtension = selectedProjectFileExtension;
        this.runnableBenderFileName = runnableBenderFileName;
        this.renderigStep = renderigStep;
        this.isEnabledAutoRunScripts = isEnabledAutoRunScripts;
    }

    public boolean isAutoFileTransfer() {
        return isAutoFileTransfer;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public int getFirstFrame() {
        return firstTask;
    }

    public int getLastFrame() {
        return lastTask;
    }

    public String getJobName() {
        return jobName;
    }

    public String getProjectFilePath() {
        return projectFilePath;
    }

    public String getOutputFolderName() {
        return outputFolderName;
    }

    public String getTaskType() {
        return taskType;
    }

    public boolean isTileEnabled() {
        return isTileEnabled;
    }

    public int getTileMultiplier() {
        return tileMultiplier;
    }

    public boolean isKeepingFoldersStructure() {
        return isKeepingFoldersStructure;
    }

    public List<String> getCommandLineScriptArgs() {
        return commandLineScriptArgs;
    }

    public FileExtensions getSelectedProjectFileExtension() {
        return selectedProjectFileExtension;
    }

    public String getRunnableBenderFileName() {
        return runnableBenderFileName;
    }

    public int getRenderigStep() {
        return renderigStep;
    }

    public boolean isEnabledAutoRunScripts() {
        return isEnabledAutoRunScripts;
    }
}