package net.whn.loki.master;

public enum FileExtensions {

    ZIP(".zip", "Zip archives with blend files"),
//    TAR(".tar", "Tar archives with blend files"),
//    GZ(".gz", "Gz archives with blend files"),
    BLEND(".blend", "Blend files");

    private final String extension;
    private final String description;

    FileExtensions(String extension, String description) {
        this.extension = extension;
        this.description = description;
    }

    public String getExtension() {
        return extension;
    }

    public String getDescription() {
        return description;
    }

    public static FileExtensions getByFileName(String fileName) {

        return fileName.endsWith(FileExtensions.ZIP.getExtension()) ? ZIP
//                : fileName.endsWith(FileExtensions.TAR.getExtension()) ? TAR
//                : fileName.endsWith(FileExtensions.GZ.getExtension()) ? GZ
                : fileName.endsWith(FileExtensions.BLEND.getExtension()) ? BLEND
                : null;
    }

    public static boolean isBlendFile(String fileName) {
        return BLEND.equals(getByFileName(fileName));
    }
}
