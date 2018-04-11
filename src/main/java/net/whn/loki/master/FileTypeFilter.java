package net.whn.loki.master;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class FileTypeFilter extends FileFilter {

    private String description;
    private List<String> extensions = new ArrayList<>();

    public FileTypeFilter(FileExtensions... fileExtensions) {

        StringBuilder builder = new StringBuilder();

        for (FileExtensions fileExtension : fileExtensions) {

            String extension = fileExtension.getExtension();
            extensions.add(extension);
            builder.append(builder.length() > 0 ? "; " : "")
                    .append(String.format("%s (*%s)", fileExtension.getDescription(), extension));
        }
        description = builder.toString();
    }

    public boolean accept(File file) {

        boolean isAcceptableFileType = false;

        if (file.isDirectory()) {
            isAcceptableFileType = true;
        } else {
            for (String extension : extensions) {
                isAcceptableFileType = file.getName().endsWith(extension);
                if (isAcceptableFileType) {
                    break;
                }
            }
        }

        return isAcceptableFileType;
    }

    public String getDescription() {
        return description;
    }
}
