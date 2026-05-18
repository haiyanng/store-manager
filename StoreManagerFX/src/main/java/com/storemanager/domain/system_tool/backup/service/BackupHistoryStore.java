package com.storemanager.domain.system_tool.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.storemanager.domain.system_tool.backup.model.BackupSummary;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BackupHistoryStore {

    private static final Path STORE_FILE =
            Path.of("data", "backup-history.json");

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .enable(SerializationFeature.INDENT_OUTPUT);

    public BackupSummary findLatest() {

        try {

            List<BackupSummary> entries = loadAll();

            if (entries.isEmpty()) {
                return null;
            }

            return entries.get(entries.size() - 1);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void append(
            BackupSummary summary
    ) {

        try {

            Files.createDirectories(STORE_FILE.getParent());

            List<BackupSummary> entries =
                    loadAll();

            entries.add(summary);

            objectMapper.writeValue(
                    STORE_FILE.toFile(),
                    entries
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot save backup history",
                    e
            );
        }
    }

    private List<BackupSummary> loadAll() {

        try {

            if (!Files.exists(STORE_FILE)) {
                return new ArrayList<>();
            }

            BackupSummary[] entries =
                    objectMapper.readValue(
                            STORE_FILE.toFile(),
                            BackupSummary[].class
                    );

            List<BackupSummary> results =
                    new ArrayList<>();

            for (BackupSummary entry : entries) {
                results.add(entry);
            }

            return results;

        } catch (Exception e) {

            e.printStackTrace();

            return new ArrayList<>();
        }
    }
}
