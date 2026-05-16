package com.storemanager.core.storage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

public class ImageStorageService {

    private static final Path DATA_DIRECTORY =
            Path.of("data");

    private static final Path IMAGE_DIRECTORY =
            DATA_DIRECTORY.resolve("images");

    public String saveProductImage(
            File sourceFile
    ) {

        return saveImage(
                sourceFile,
                "product"
        );
    }

    public String saveCategoryImage(
            File sourceFile
    ) {

        return saveImage(
                sourceFile,
                "category"
        );
    }

    public String saveEmployeeImage(
            File sourceFile
    ) {

        return saveImage(
                sourceFile,
                "employee"
        );
    }

    public File resolveImageFile(
            String relativePath
    ) {

        if (relativePath == null || relativePath.trim().isEmpty()) {
            return null;
        }

        Path path =
                Path.of(relativePath.trim())
                        .normalize();

        if (path.isAbsolute() || !path.startsWith(DATA_DIRECTORY)) {
            return null;
        }

        return path.toFile();
    }

    private String saveImage(
            File sourceFile,
            String folderName
    ) {

        if (sourceFile == null || !sourceFile.isFile()) {
            throw new RuntimeException(
                    "Image file is required"
            );
        }

        try {

            Path targetDirectory =
                    IMAGE_DIRECTORY.resolve(folderName);

            Files.createDirectories(targetDirectory);

            String fileName =
                    UUID.randomUUID()
                            + getSafeExtension(sourceFile);

            Path targetPath =
                    targetDirectory.resolve(fileName);

            Files.copy(
                    sourceFile.toPath(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return DATA_DIRECTORY
                    .relativize(targetPath)
                    .toString()
                    .replace(File.separatorChar, '/')
                    .replaceFirst("^", "data/");

        } catch (Exception e) {

            throw new RuntimeException(
                    "Cannot save image",
                    e
            );
        }
    }

    private String getSafeExtension(
            File sourceFile
    ) {

        String name =
                sourceFile.getName();

        int dotIndex =
                name.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == name.length() - 1) {
            return ".png";
        }

        String extension =
                name.substring(dotIndex)
                        .toLowerCase(Locale.ROOT);

        return switch (extension) {
            case ".png", ".jpg", ".jpeg", ".gif", ".bmp" -> extension;
            default -> ".png";
        };
    }
}
