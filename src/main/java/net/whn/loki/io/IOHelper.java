package net.whn.loki.io;

import net.whn.loki.common.EQCaller;
import net.whn.loki.common.LokiForm;
import net.whn.loki.common.Main;
import net.whn.loki.common.ProjectFileObject;
import net.whn.loki.common.configs.Config;
import net.whn.loki.grunt.GruntR;
import net.whn.loki.master.MasterR;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class IOHelper {

    private static final Logger log = Logger.getLogger(IOHelper.class.toString());
    public static final int BUFFER_SIZE = 8192;
    protected static long start;
    public static String userHomeFolder = System.getProperty("user.home");
    private static String failedToCreateFolderTemplate = "couldn't create directory: ";
    private static String failedToWriteInFolderTemplate = "couldn't write to directory: ";
    private static String readWriteMessageSuggestion = getReadWriteMessageSuggestion();

    private static String getReadWriteMessageSuggestion() {
        return new StringBuilder("Please give Loki read/write permissions to the")
                .append(System.lineSeparator())
                .append("directory: '").append(userHomeFolder).append("'")
                .append(System.lineSeparator())
                .append("and restart Loki.")
                .toString();
    }

    /**
     * Used when loki is started from GUI
     * @param lokiForm
     */
    public static void handleUnwrittableFileSystemException(LokiForm lokiForm) {

        EQCaller.showMessageDialog(lokiForm, "Loki needs write permissions", readWriteMessageSuggestion, JOptionPane.WARNING_MESSAGE);
        log.severe("filesystem is not writable. Loki exiting.");
    }

    /**
     * Used when Loki is started from command line
     */
    public static void handleUnwrittableFileSystemException() {

        System.out.println(readWriteMessageSuggestion);
        log.severe("filesystem is not writable. Loki exiting.");
    }

    /**
     * Creates folder only if it doesn't exist
     * @param folder
     * @return
     */
    private static File getFolder(File folder) {

        if (!folder.exists() && !folder.mkdir()) {
            log.severe(String.format(failedToCreateFolderTemplate, folder));
            return null;
        }

        return folder;
    }

    /**
     * Check only this up level folder if it's writable.
     * If it's true, then all the system inside is writable.
     * @return
     */
    public static File getLokiBaseFolder() {

        File folder = getFolder(new File(userHomeFolder, Main.LOKI_BASE_FOLDER));
        if (folder != null) {
            if (isFolderWritable(folder)) {
                return folder;
            } else {
                log.severe(String.format(failedToWriteInFolderTemplate, folder));
            }
        }
        return null;
    }

    public static File getGruntMainFolder(File lokiBaseFolder) {

        return getFolder(new File(lokiBaseFolder, GruntR.MAIN_FOLDER_NAME));
    }

    public static File getGruntCacheFolder(File lokiBaseFolder) {

        return getFolder(new File(lokiBaseFolder, GruntR.CACHE_FOLDER_NAME));
    }

    public static File getGruntRenderFolder(File lokiBaseFolder) {

        return getFolder(new File(lokiBaseFolder, GruntR.RENDER_FOLDER_NAME));
    }

    public static File getMasterMainFolder(File lokiBaseFolder) {

        return getFolder(new File(lokiBaseFolder, MasterR.MAIN_FOLDER_NAME));
    }

    public static File getMasterCacheFolder(File lokiBaseFolder) {

        return getFolder(new File(lokiBaseFolder, MasterR.CACHE_FOLDER_NAME));
    }

    public static File getMasterTempFolder(File lokiBaseFolder) {

        return getFolder(new File(lokiBaseFolder, MasterR.TEMP_FOLDER_NAME));
    }

    public static void deleteRunningLock() {
        File runningFile = new File(Main.lokiBaseFolder, ".runningLock");
        if (runningFile.exists())
            runningFile.delete();
    }

    /**
     * generates MD5 for given file.
     * @param oFile
     * @return md5 as hex string, or null if failed
     * @throws IOException
     */
    public static String generateMD5(File oFile) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        InputStream inFile = null;
        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance("MD5");
            inFile = new FileInputStream(oFile);

            int amountRead;
            while (true) {
                amountRead = inFile.read(buffer);
                if (amountRead == -1) {
                    break;
                }
                digest.update(buffer, 0, amountRead);
            }
            return binToHex(digest.digest());

        } catch (NoSuchAlgorithmException ex) {
            log.severe("md5 algorithm not available!");
        } catch (FileNotFoundException ex) {
            log.severe("file not found: " + ex.getMessage());
        } finally {
            if (inFile != null) {
                inFile.close();
            }
        }
        return null;
    }

    public static long getFileCacheSize(ConcurrentHashMap<String, ProjectFileObject> fileCacheMap) {
        long total = 0;
        ProjectFileObject currentpFile;
        Iterator it = fileCacheMap.entrySet().iterator();
        Map.Entry pair;
        while (it.hasNext()) {
            pair = (Map.Entry) it.next();
            currentpFile = (ProjectFileObject) pair.getValue();
            total += currentpFile.getSize();
        }
        return total;
    }

    /**
     *
     * @param lokiBaseFolder
     * @return true if .runningLock file didn't exist, otherwise true
     * @throws IOException
     */
    public static boolean couldSetupRunningLock(File lokiBaseFolder) throws IOException {

        File runningLock = new File(lokiBaseFolder, ".runningLock");

        if (runningLock.createNewFile()) {
            runningLock.deleteOnExit();
            return true;
        } else {
            return false;   //oops; loki is already running on this system
        }
    }

    /**
     * zips up a given directory. skips subdirectories!
     * @param dir
     * @param outputZipFile
     * @return
     */
    public static boolean zipDirectory(File dir, File outputZipFile) {
        if (outputZipFile.exists()) {
            outputZipFile.delete();
            log.warning(outputZipFile.toString() + " already exists: overwriting");
        }
        IOHelper.start = System.currentTimeMillis();
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputZipFile));
            out.setLevel(1);
            addDir(dir, out);
            out.close();
            log.info("zipped blendcache in (ms): " + (System.currentTimeMillis() - IOHelper.start));
            return true;
        } catch (Exception ex) {
            log.warning("failed to zip blendcache directory: " + ex.toString());
        }
        return false;
    }

    public static void unzipFile(File zipFile, File outputFolder) throws IOException {

        byte[] buffer = new byte[1024];
        FileUtils.forceMkdir(outputFolder);

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {

                String entryName = zipEntry.getName();
                File zipElement = new File(outputFolder, entryName);

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(zipElement.toPath());
                } else {
                    try (FileOutputStream fileOutputStream = new FileOutputStream(zipElement)) {
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, length);
                        }
                    }
                }
            }
            zipInputStream.closeEntry();
        } catch (IOException e) {
            log.warning("failed to unpack zip file: " + zipFile.toString());
            throw e;
        }
    }

    /**
     * Deletes a directory recursively.
     * @param folder
     */
    public static void deleteFolder(File folder) {
        try {
            FileUtils.deleteDirectory(folder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String generateBlendCacheDirName(String blendFileName) {
        int dotIndex = blendFileName.lastIndexOf('.');

        if (dotIndex != -1) {
            blendFileName = blendFileName.substring(0, dotIndex);
        }

        return "blendcache_" + blendFileName;
    }


    /**
     * converts bytes to a hex string
     * @param bytes
     * @return
     */
    protected static String binToHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }

    protected static void addProjectFileToCache(File projectFile, Config config) {

        ConcurrentHashMap<String, ProjectFileObject> gruntFileCacheMap = config.getGruntFileCacheMap();
        String name = projectFile.getName();

        if (!gruntFileCacheMap.containsKey(name)) {

            ProjectFileObject projectFileObject = new ProjectFileObject(projectFile);
            gruntFileCacheMap.put(name, projectFileObject);

            if (projectFileObject.getSize() > config.getCacheSizeLimitBytes()) {
                config.setCacheSizeLimitBytes((projectFileObject.getSize() * 4));
                log.info("increasing cache limit to: " + projectFileObject.getSize() * 4);
            }
            manageCacheLimit(gruntFileCacheMap, config);
        }
    }

    /**
     * Should be called after a new file has been added to the cache.
     * If we're over the limit, should iteratively remove oldest used files
     * until we meet the limit constraint.
     */
    protected static void manageCacheLimit(ConcurrentHashMap<String, ProjectFileObject> fileCacheMap, Config config) {

        //we need to delete files using a "longest ago used" algorithm
        //until we fall under the limit
        if (!fileCacheMap.isEmpty()) {
            while (config.getCacheSize() > config.getCacheSizeLimitBytes()) {
                ProjectFileObject oldestProjectFileObject = null;
                Iterator iterator = fileCacheMap.entrySet().iterator();


                /**
                 * Make something like:
                 * Sun Feb 11 09:24:20 EET 2018 // System.currentTimeMillis()
                 * Sun Feb 11 09:41:00 EET 2018 // lowestTime
                 * The addition of million moves time ahead with +- 17 min 20 sec
                 */
                long lowestTime = System.currentTimeMillis() + 1_000_000;

                while (iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    ProjectFileObject currentProjectFileObject = (ProjectFileObject) entry.getValue();
                    if (currentProjectFileObject.getTimeLastUsed() < lowestTime) {
                        oldestProjectFileObject = currentProjectFileObject;
                        lowestTime = oldestProjectFileObject.getTimeLastUsed();
                    }
                }
                //we now have our delete candidate, so delete it.

                if (!oldestProjectFileObject.isInUse() && config.getJobsModel().isProjectFileOrphaned(oldestProjectFileObject.getName())) {
                    if (!oldestProjectFileObject.getFile().delete()) {
                        log.severe("failed to delete cache file");
                    }
                    fileCacheMap.remove(oldestProjectFileObject.getName());
                    log.finer("deleting file: " + oldestProjectFileObject.getName());
                } else {
                    log.fine("manageCacheLimit wanted to delete file in use!");
                    break;
                }
            }
        }
    }

    private static boolean isFolderWritable(File lokiBaseFolder) {
        try {
            return couldCreateAndDeleteFileIntoFolder(lokiBaseFolder) && couldCreateAndDeleteFolderIntoFolder(lokiBaseFolder);
        } catch (IOException ex) {
            log.severe("could not write to directory!" + ex.getMessage());
            return false;
        }
    }

    private static boolean couldCreateAndDeleteFileIntoFolder(File folder) throws IOException {

        File tempFile = new File(folder, "loki.tmp");
        return tempFile.createNewFile() && tempFile.delete();
    }

    private static boolean couldCreateAndDeleteFolderIntoFolder(File folder) {

        File tempFolder = new File(folder, "lokiDir");
        return tempFolder.mkdir() && tempFolder.delete();
    }

    private static void addDir(File dir, ZipOutputStream out) throws IOException {
        File[] files = dir.listFiles();
        byte[] tmpBuf = new byte[4096];
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                //addDir(files[i], out);
                continue;
            }
            FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
            //out.putNextEntry(new ZipEntry(files[i].getAbsolutePath()));
            out.putNextEntry(new ZipEntry(files[i].getName()));
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
    }
}
