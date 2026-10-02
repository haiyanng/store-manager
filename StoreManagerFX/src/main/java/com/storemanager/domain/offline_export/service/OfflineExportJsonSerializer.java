package com.storemanager.domain.offline_export.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.storemanager.domain.offline_export.OfflineExportSpecV1;
import com.storemanager.domain.offline_export.dto.OfflineExportBundle;
import com.storemanager.domain.offline_export.dto.OfflineExportManifest;

import java.nio.file.Files;
import java.nio.file.Path;

public class OfflineExportJsonSerializer {

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .setSerializationInclusion(
                            JsonInclude.Include.NON_NULL
                    )
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .enable(SerializationFeature.INDENT_OUTPUT);

    public void writePackage(
            Path exportRoot,
            OfflineExportBundle bundle,
            OfflineExportManifest manifest
    ) {

        writeSnapshots(
                exportRoot,
                bundle
        );
        writeManifest(
                exportRoot,
                manifest
        );
    }

    public void writeSnapshots(
            Path exportRoot,
            OfflineExportBundle bundle
    ) {

        writeJson(
                exportRoot.resolve(OfflineExportSpecV1.USERS_FILE),
                bundle.users()
        );
        writeJson(
                exportRoot.resolve(OfflineExportSpecV1.EMPLOYEES_FILE),
                bundle.employees()
        );
        writeJson(
                exportRoot.resolve(OfflineExportSpecV1.CATEGORIES_FILE),
                bundle.categories()
        );
        writeJson(
                exportRoot.resolve(OfflineExportSpecV1.PRODUCTS_FILE),
                bundle.products()
        );
        writeJson(
                exportRoot.resolve(OfflineExportSpecV1.INVENTORY_FILE),
                bundle.inventory()
        );
        writeJson(
                exportRoot.resolve(OfflineExportSpecV1.ATTENDANCE_FILE),
                bundle.attendance()
        );
        writeJson(
                exportRoot.resolve(OfflineExportSpecV1.SALES_FILE),
                bundle.sales()
        );
        writeJson(
                exportRoot.resolve(OfflineExportSpecV1.SALE_ITEMS_FILE),
                bundle.saleItems()
        );
        writeJson(
                exportRoot.resolve(OfflineExportSpecV1.AUDIT_LOGS_FILE),
                bundle.auditLogs()
        );
    }

    public void writeManifest(
            Path exportRoot,
            OfflineExportManifest manifest
    ) {

        writeJson(
                exportRoot.resolve(OfflineExportSpecV1.MANIFEST_FILE),
                manifest
        );
    }

    private void writeJson(
            Path file,
            Object value
    ) {

        try {

            Files.createDirectories(file.getParent());

            objectMapper.writeValue(
                    file.toFile(),
                    value
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Cannot write export JSON: " + file,
                    e
            );
        }
    }
}
