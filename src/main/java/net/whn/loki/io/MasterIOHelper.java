package net.whn.loki.io;

import net.whn.loki.brokersModel.Broker;
import net.whn.loki.common.*;
import net.whn.loki.common.configs.Config;
import net.whn.loki.error.MasterFrozenException;
import net.whn.loki.grunt.GruntR;
import net.whn.loki.master.Job;
import net.whn.loki.master.MasterR;
import net.whn.loki.messaging.FileReceivedMessage;
import net.whn.loki.network.BrokerStreamSocket;
import net.whn.loki.network.Header;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class MasterIOHelper extends IOHelper {

    private static final Logger log = Logger.getLogger(MasterIOHelper.class.toString());

    public static Job copyJob(Job j) throws IOException, ClassNotFoundException {

        Job job = null;
        byte[] buffer = null;

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {

            objectOutputStream.writeObject(j);
            buffer = outputStream.toByteArray();
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer);
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {

            job = (Job) objectInputStream.readObject();
        }
        return job;
    }

    /**
     * master calls this when it got a new job; needs to add file to cache
     * Maybe cache is needed to not send twice the same blend file to the Grunt machine
     * @param fileCacheMap
     * @param projectFile like: "C:\Users\angel\Desktop\BMW27GE.blend"
     * @param config
     * @param lokiCacheFolder
     */
    public static void addProjectFileToCache(ConcurrentHashMap<String, ProjectFileObject> fileCacheMap, File projectFile, Config config, File lokiCacheFolder) {

        String projectFileName = projectFile.getName();
        if (!fileCacheMap.containsKey(projectFileName)) {
            if (!copyProjectFileToMasterCacheFolder(projectFile, lokiCacheFolder, projectFileName)) {
                return;
            }
            addProjectFileToCacheMap(fileCacheMap, projectFile, config, projectFileName);
        }
    }

    private static void addProjectFileToCacheMap(ConcurrentHashMap<String, ProjectFileObject> fileCacheMap, File projectFile, Config config, String projectFileName) {

        ProjectFileObject projectFileObject = new ProjectFileObject(projectFile);
        fileCacheMap.put(projectFileName, projectFileObject);

        if (projectFileObject.getSize() > config.getCacheSizeLimitBytes()) {
            long limitBytes = projectFileObject.getSize() * 4;
            config.setCacheSizeLimitBytes(limitBytes);
            log.info("increasing cache limit to: " + limitBytes);
        }
        manageCacheLimit(fileCacheMap, config);
    }

    /**
     * @param projectFile
     * @param lokiCacheFolder
     * @param projectFileName
     * @return - true if file was successfully copied, otherwise false
     */
    private static boolean copyProjectFileToMasterCacheFolder(File projectFile, File lokiCacheFolder, String projectFileName) {

        byte[] buffer = new byte[BUFFER_SIZE];
        File cacheProjectFile = new File(lokiCacheFolder, projectFileName);

        try (InputStream inputStream = new FileInputStream(projectFile);
             OutputStream outputStream = new FileOutputStream(cacheProjectFile)) {

            int amountRead;
            while ((amountRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, amountRead);
            }

        } catch (Exception ex) {
            log.severe("failed to cache project file! " + ex.getMessage());
            return false;
        }
        return true;
    }

    public static void sendProjectFileToGrunt(ProjectFileObject projectFileObject, BrokerStreamSocket bSSock) throws IOException {

        projectFileObject.setInUse(true);
        byte[] buffer = new byte[BUFFER_SIZE];
        File file = projectFileObject.getFile();

        try (InputStream inFile = new FileInputStream(file)) {
            int amountRead;
            while ((amountRead = inFile.read(buffer)) != -1) {

                bSSock.sendProjectFileChunk(buffer, amountRead);
            }
            bSSock.flushSockOut();
        } catch (FileNotFoundException ex) {
            log.severe("file to send not found: " + file.getAbsolutePath());
        }

        projectFileObject.setInUse(false);
    }

    /**
     * Create parent folders in Master output folder for the given file from Grunt
     * , if is needed, and return new file with this path
     * @param file
     * @param isKeepingFoldersStructure
     */
    public static File getFileForOutput(File file,
                                         String outputFolder,
                                         String outputFileName,
                                         boolean isKeepingFoldersStructure) {

        File outputFile = null;

        if (!isKeepingFoldersStructure || isInGruntRenderFolder(file)) { // move directly to output folder:
            outputFile = new File(outputFolder, outputFileName);

        } else {
            StringBuilder folderStructure = new StringBuilder();

            if (isInGruntFileCacheFolder(file)) {

                List<String> parentFolders = new ArrayList<>();
                File parentFolder = file.getParentFile();

                while (true) {
                    if (parentFolder.equals(GruntR.CACHE_FOLDER)) {
                        break;
                    } else {
                        parentFolders.add(parentFolder.getName());
                    }
                    parentFolder = parentFolder.getParentFile();
                }

                for (ListIterator<String> it = parentFolders.listIterator(parentFolders.size()); it.hasPrevious();) {
                    folderStructure.append(it.previous()).append(File.separator);
                }
            }
            folderStructure.append(file.getName());
            outputFile = new File(outputFolder, folderStructure.toString());
        }
        File directory = new File(outputFile.getParentFile().getAbsolutePath());
        directory.mkdirs();

        return outputFile;
    }

    private static boolean isInGruntFileCacheFolder(File file) {

        String gruntFileCachePath = new StringBuilder(Main.LOKI_BASE_FOLDER).append(File.separator)
                .append(GruntR.MAIN_FOLDER_NAME) .append(File.separator)
                .append(GruntR.CACHE_FOLDER_NAME)
                .toString();

        return file.getPath().contains(gruntFileCachePath);
    }

    private static boolean isInGruntRenderFolder(File file) {

        String standardEndPath = new StringBuilder(Main.LOKI_BASE_FOLDER).append(File.separator)
                .append(GruntR.MAIN_FOLDER_NAME) .append(File.separator)
                .append(GruntR.RENDER_FOLDER_NAME) .append(file.getName())
                .toString();

        return file.getPath().endsWith(standardEndPath);
    }

    /**
     * receives output files from grunt via network, and adds to job's output dir.
     * if a tile, then puts in '<job>-<frame>' folder in tmp
     */
    public static boolean receiveOutputFilesFromGrunt(Task task, Broker broker) {

        BrokerStreamSocket brokerStreamSocket = broker.getBrokerStreamSocket();
        BlockingQueue<Header> queue = broker.getHeaderBlockingQueue();

        if (task.isTile()) {
            long remaining = task.getRenderedFileAttributes().get(0).getFileLength(); // here we have just on file
            byte[] buffer = new byte[BUFFER_SIZE];

            boolean isFileReceived = false;
            try (OutputStream outputStream = new FileOutputStream(getOutputTiledFile(task))) {

                int amountRead;
                InputStream sockIn = brokerStreamSocket.getSockIn();

                while (remaining > 0) {
                    amountRead = remaining > BUFFER_SIZE ? sockIn.read(buffer) //read up to a full buffer
                            : sockIn.read(buffer, 0, (int) remaining);

                    outputStream.write(buffer, 0, amountRead);
                    outputStream.flush();
                    remaining -= amountRead;
                }
                outputStream.flush();
                isFileReceived = true;

                handleMachineUpdate(broker, queue);

            } catch (Exception e) {
                log.severe("failed during receive/save of output file! " + e.getMessage());
                return false;
            }

            // specially send this flag apart, to avoid exception thrown when Grunt and Master are both on the same machine
            // The process cannot access the file because it is being used by another process.
            if (isFileReceived) {
                try {
                    brokerStreamSocket.sendFileReceiveHeader();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {
            // rendered files will be sent exactly in the same order
            for (RenderedFileAttribute attribute : task.getRenderedFileAttributes()) {

                File file = attribute.getFile();
                File fileForOutput = getFileForOutput(file,
                        task.getOutputFolderName(),
                        task.getOutputFilePrefix() + file.getName()
                        , task.isKeepingFoldersStructure()
                );

                byte[] buffer = new byte[BUFFER_SIZE];
                boolean isFileReceived = false;

                try (OutputStream outputStream = new FileOutputStream(fileForOutput)) {

                    long remaining = attribute.getFileLength();
                    int amountRead;
                    InputStream sockIn = brokerStreamSocket.getSockIn();

                    while (remaining > 0) {
                        amountRead = remaining > BUFFER_SIZE ? sockIn.read(buffer) //read up to a full buffer
                            : sockIn.read(buffer, 0, (int) remaining);

                        outputStream.write(buffer, 0, amountRead);
                        outputStream.flush(); // pt orice eventualitate
                        remaining -= amountRead;
                    }
                    outputStream.flush();
                    isFileReceived = true;

                    handleMachineUpdate(broker, queue);

                } catch (Exception e) {
                    log.severe("failed during receive/save of output file! " + e.getMessage());
                    return false;
                }

                // specially send this flag apart, to avoid exception thrown when Grunt and Master are both on the same machine
                // The process cannot access the file because it is being used by another process.
                if (isFileReceived) {
                    try {
                        brokerStreamSocket.sendFileReceiveHeader();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        try {
            broker.getMaster().deliverMessage(new FileReceivedMessage(task.getGruntId()));
        } catch (InterruptedException | MasterFrozenException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Handle possible MACHINE_UPDATE, what could happen during file receiving
     * @param broker
     * @param queue
     * @throws InterruptedException
     */
    private static void handleMachineUpdate(Broker broker, BlockingQueue<Header> queue) throws InterruptedException {
        Header header = queue.peek();
        if (header != null && header.getHeaderType().equals(ICommon.HeaderType.MACHINE_UPDATE)) {
            try {
                broker.handleMachineUpdate(queue.take());
            } catch (MasterFrozenException e) {
                e.printStackTrace();
            }
        }
    }

    private static File getOutputTiledFile(Task task) throws IOException {
        return new File(makeOutputTileFolder(task), Integer.toString(task.getTile()) + task.getGeneratedOutputFileExtension());
    }

    private static File makeOutputTileFolder(Task task) throws IOException {
        File outputFolder = getJobIdFrameFolder(task);
        FileUtils.forceMkdir(outputFolder);
        return outputFolder;
    }

    private static File getJobIdFrameFolder(Task task) {
        return new File(MasterR.TEMP_FOLDER, Long.toString(task.getJobId()) + "-" + Integer.toString(task.getFrame()));
    }
}
