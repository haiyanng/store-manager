package com.storemanager.domain.audit.repository;

import com.storemanager.domain.audit.model.AuditLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuditLogFileRepository {

    private static final Logger LOGGER =
            Logger.getLogger(AuditLogFileRepository.class.getName());

    // AuditService has multiple instances, including ones used by background tasks.
    private static final Object WRITE_LOCK = new Object();

    private static final Path LOG_DIRECTORY = Path.of("logs");

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public boolean save(AuditLog log) {
        LocalDateTime timestamp = log.getCreatedAt() == null
                ? LocalDateTime.now()
                : log.getCreatedAt();
        Path file = LOG_DIRECTORY.resolve("audit-" + timestamp.toLocalDate() + ".txt");
        String entry = timestamp.format(TIMESTAMP)
                + " | user=" + singleLine(log.getActorUsername())
                + " | user_id=" + singleLine(log.getUserId())
                + " | module=" + singleLine(log.getModule())
                + " | action=" + singleLine(log.getAction())
                + " | target=" + singleLine(log.getEntityType())
                + " | target_id=" + singleLine(log.getEntityId())
                + " | result=" + (Boolean.TRUE.equals(log.getSuccess()) ? "SUCCESS" : "FAILED")
                + " | branch_id=" + singleLine(log.getBranchId())
                + " | reason=" + singleLine(log.getReason())
                + " | details=" + singleLine(log.getDetailsJson() == null
                        ? log.getDetails() : log.getDetailsJson())
                + System.lineSeparator();

        try {
            synchronized (WRITE_LOCK) {
                Files.createDirectories(LOG_DIRECTORY);
                try (FileChannel channel = FileChannel.open(file,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     FileLock lock = channel.lock()) {
                    // Also serialize writes from separate running application instances.
                    channel.position(channel.size());
                    ByteBuffer bytes = StandardCharsets.UTF_8.encode(entry);
                    while (bytes.hasRemaining()) {
                        channel.write(bytes);
                    }
                    channel.force(false);
                }
            }
            return true;
        } catch (IOException | SecurityException e) {
            // A disk/permission failure must not prevent the database audit attempt.
            LOGGER.log(Level.WARNING, "Cannot append audit log to " + file.toAbsolutePath(), e);
            return false;
        }
    }

    private String singleLine(Object value) {
        if (value == null) {
            return "-";
        }
        StringBuilder result = new StringBuilder();
        for (char character : value.toString().toCharArray()) {
            switch (character) {
                case '\\' -> result.append("\\\\");
                case '|' -> result.append("\\|");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (Character.isISOControl(character)
                            || character == '\u2028' || character == '\u2029') {
                        result.append(String.format("\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.toString();
    }
}
