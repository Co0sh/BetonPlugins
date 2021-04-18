package org.betonquest.betonquest.utils;

import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@CustomLog(topic = "Zipper")
public class Zipper {
    private static final String ZIP_EXTENSION = ".zip";
    private final List<File> fileList;

    public Zipper(@NotNull final File sourceFile, @NotNull final String outputPath) {
        fileList = new ArrayList<>();
        generateFileList(sourceFile);
        zipIt(getOutputFile(outputPath));
    }

    public Zipper(@NotNull final List<File> sourceFiles, @NotNull final String outputPath) {
        fileList = sourceFiles;
        zipIt(getOutputFile(outputPath));
    }

    private void generateFileList(@NotNull final File file) {
        if (file.isFile()) {
            fileList.add(file);
        } else {
            final File[] subFiles = file.listFiles();
            if (subFiles == null) {
                LOG.error(null, "Could not get files for directory '" + file.getPath() + "'!");
                return;
            }
            for (final File subFile : subFiles) {
                generateFileList(subFile);
            }
        }
    }

    @NotNull
    private File getOutputFile(@NotNull final String output) {
        File file = new File(output + ZIP_EXTENSION);
        int counter = 1;
        while (file.exists()) {
            counter++;
            file = new File(output + "-" + counter + ZIP_EXTENSION);
        }
        return file;
    }

    private void zipIt(@NotNull final File zipFile) {
        final byte[] buffer = new byte[1024];

        try (OutputStream fos = Files.newOutputStream(Paths.get(zipFile.toURI()));
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (final File file : this.fileList) {
                final ZipEntry zipEntry = new ZipEntry(file.getName());
                zos.putNextEntry(zipEntry);
                try (InputStream input = Files.newInputStream(Paths.get(file.toURI()))) {
                    int len = input.read(buffer);
                    while (len > 0) {
                        zos.write(buffer, 0, len);
                        len = input.read(buffer);
                    }
                }
            }
            zos.closeEntry();
        } catch (final IOException e) {
            LOG.warning(null, "Could not zip the files to '" + zipFile.getPath() + "'!", e);
        }
    }
}
