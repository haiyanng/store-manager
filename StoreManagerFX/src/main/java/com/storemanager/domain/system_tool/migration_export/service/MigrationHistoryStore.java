package com.storemanager.domain.system_tool.migration_export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.storemanager.domain.system_tool.migration_export.model.MigrationHistoryEntry;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MigrationHistoryStore {

    private static final Path STORE_FILE =
            Path.of("data", "migration-history.json");

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .enable(SerializationFeature.INDENT_OUTPUT);

    public List<MigrationHistoryEntry> loadAll() {

        try {

            if (!Files.exists(STORE_FILE)) {
                return new ArrayList<>();
            }

            MigrationHistoryEntry[] entries =
                    objectMapper.readValue(
                            STORE_FILE.toFile(),
                            MigrationHistoryEntry[].class
                    );

            List<MigrationHistoryEntry> results =
                    new ArrayList<>();

            Collections.addAll(results, entries);

            return results;

        } catch (Exception e) {

            e.printStackTrace();

            return new ArrayList<>();
        }
    }

    public void append(
            MigrationHistoryEntry entry
    ) {

        try {

            Files.createDirectories(
                    STORE_FILE.getParent()
            );

            List<MigrationHistoryEntry> entries =
                    loadAll();

            entries.add(entry);

            objectMapper.writeValue(
                    STORE_FILE.toFile(),
                    entries
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Cannot save migration history",
                    e
            );
        }
    }
}
