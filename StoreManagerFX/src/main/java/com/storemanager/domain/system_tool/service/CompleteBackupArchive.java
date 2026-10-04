package com.storemanager.domain.system_tool.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** File format for disaster recovery; deliberately excludes local database connection settings. */
public class CompleteBackupArchive {

    public static final String SQL_ENTRY = "database.sql";
    private static final String FORMAT_ENTRY = "backup-format.txt";
    private static final String FORMAT = "STOREMANAGER_COMPLETE_BACKUP_V1";
    private static final String IMAGE_PREFIX = "data/images/";

    public void write(Path sqlFile, Path imageDirectory, Path output) throws IOException {
        if (!Files.isRegularFile(sqlFile) || Files.size(sqlFile) == 0) {
            throw new IOException("The database backup is empty");
        }
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            zip.putNextEntry(new ZipEntry(FORMAT_ENTRY));
            zip.write(FORMAT.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            addFile(zip, sqlFile, SQL_ENTRY);
            zip.putNextEntry(new ZipEntry(IMAGE_PREFIX));
            zip.closeEntry();
            if (Files.exists(imageDirectory)) {
                if (Files.isSymbolicLink(imageDirectory) || !Files.isDirectory(imageDirectory)) {
                    throw new IOException("The image directory must be a regular directory");
                }
                try (var paths = Files.walk(imageDirectory)) {
                    for (Path path : paths.sorted().toList()) {
                        if (Files.isSymbolicLink(path)) {
                            throw new IOException("Cannot back up a symbolic link in the image directory: " + path);
                        }
                        if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                            String name = IMAGE_PREFIX + imageDirectory.relativize(path).toString().replace('\\', '/');
                            addFile(zip, path, name);
                        }
                    }
                }
            }
        }
    }

    /** Extracts and validates every entry before the caller changes the live database. */
    public Path extract(Path archive, Path stagingDirectory) throws IOException {
        Path root = stagingDirectory.toAbsolutePath().normalize();
        Files.createDirectories(root.resolve("data/images"));
        Set<String> names = new HashSet<>();
        boolean formatFound = false;
        boolean sqlFound = false;
        boolean imagesFound = false;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                Path target = safeTarget(root, name);
                if (!names.add(name)) {
                    throw new IOException("Duplicate backup entry: " + name);
                }
                if (FORMAT_ENTRY.equals(name)) {
                    String format = new String(zip.readNBytes(128), StandardCharsets.UTF_8);
                    if (!FORMAT.equals(format)) {
                        throw new IOException("Unsupported complete backup format");
                    }
                    formatFound = true;
                } else if (SQL_ENTRY.equals(name) && !entry.isDirectory()) {
                    Files.copy(zip, target);
                    sqlFound = Files.size(target) > 0;
                } else if (name.startsWith(IMAGE_PREFIX)) {
                    imagesFound = true;
                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(zip, target);
                    }
                } else {
                    throw new IOException("Unexpected backup entry: " + name);
                }
                zip.closeEntry();
            }
        }
        if (!formatFound || !sqlFound || !imagesFound) {
            throw new IOException("A complete backup requires its format marker, database.sql and data/images");
        }
        return root.resolve(SQL_ENTRY);
    }

    private Path safeTarget(Path root, String name) throws IOException {
        if (name == null || name.isBlank() || name.contains("\\") || name.contains(":") || name.startsWith("/")) {
            throw new IOException("Invalid backup path: " + name);
        }
        for (String segment : name.split("/")) {
            if (segment.equals("..") || segment.equals(".")) {
                throw new IOException("Invalid backup path: " + name);
            }
        }
        Path target = root.resolve(name).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new IOException("Backup path escapes its staging directory: " + name);
        }
        return target;
    }

    /** Uses directory moves on the same volume so a copy error leaves the previous image set intact. */
    public void restoreImages(Path extractedImageDirectory, Path liveImageDirectory) throws IOException {
        Path destination = liveImageDirectory.toAbsolutePath().normalize();
        Files.createDirectories(destination.getParent());
        Path prepared = Files.createTempDirectory(destination.getParent(), "images-restore-");
        Path previous = null;
        boolean published = false;
        try {
            try (var paths = Files.walk(extractedImageDirectory)) {
                for (Path path : paths.toList()) {
                    Path target = prepared.resolve(extractedImageDirectory.relativize(path));
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(target);
                    } else {
                        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            if (Files.exists(destination)) {
                if (Files.isSymbolicLink(destination) || !Files.isDirectory(destination)) {
                    throw new IOException("The live image directory must be a regular directory");
                }
                previous = Files.createTempDirectory(destination.getParent(), "images-previous-");
                Files.delete(previous);
                Files.move(destination, previous);
            }
            try {
                Files.move(prepared, destination);
                published = true;
            } catch (IOException e) {
                if (previous != null) {
                    Files.move(previous, destination);
                    previous = null;
                }
                throw e;
            }
        } finally {
            deleteTemporaryDirectory(prepared);
            if (published) {
                deleteTemporaryDirectory(previous);
            }
        }
    }

    public static void deleteTemporaryDirectory(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { /* Best effort. */ }
            }
        } catch (IOException ignored) { /* Best effort. */ }
    }

    private void addFile(ZipOutputStream zip, Path file, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Files.copy(file, zip);
        zip.closeEntry();
    }
}
