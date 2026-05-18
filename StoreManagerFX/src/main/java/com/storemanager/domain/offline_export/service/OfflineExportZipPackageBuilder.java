package com.storemanager.domain.offline_export.service;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Stream;

public class OfflineExportZipPackageBuilder {

    public void build(
            Path sourceDirectory,
            Path outputZip
    ) {

        try {

            Path parent = outputZip.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (
                    OutputStream outputStream =
                            Files.newOutputStream(outputZip);
                    ZipOutputStream zipOutputStream =
                            new ZipOutputStream(outputStream)
            ) {

                try (Stream<Path> paths = Files.walk(sourceDirectory)) {
                    paths.sorted(Comparator.naturalOrder())
                            .filter(path -> !path.equals(sourceDirectory))
                            .forEach(path -> addEntry(
                                    zipOutputStream,
                                    sourceDirectory,
                                    path
                            ));
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Cannot build export zip package",
                    e
            );
        }
    }

    private void addEntry(
            ZipOutputStream zipOutputStream,
            Path sourceDirectory,
            Path path
    ) {

        try {

            String entryName =
                    sourceDirectory.relativize(path)
                            .toString()
                            .replace('\\', '/');

            if (Files.isDirectory(path)) {
                if (!entryName.endsWith("/")) {
                    entryName = entryName + "/";
                }

                zipOutputStream.putNextEntry(
                        new ZipEntry(entryName)
                );
                zipOutputStream.closeEntry();
                return;
            }

            zipOutputStream.putNextEntry(
                    new ZipEntry(entryName)
            );
            Files.copy(
                    path,
                    zipOutputStream
            );
            zipOutputStream.closeEntry();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Cannot add export entry: " + path,
                    e
            );
        }
    }
}
