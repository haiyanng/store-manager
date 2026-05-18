package com.storemanager.domain.system_tool.migration_export.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MigrationPreviewResult {

    private String exportVersion;
    private String sourceSystem;
    private String appVersion;
    private String businessName;
    private Map<String, Integer> recordCounts;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> validationErrors = new ArrayList<>();

    public String getExportVersion() {
        return exportVersion;
    }

    public void setExportVersion(String exportVersion) {
        this.exportVersion = exportVersion;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public Map<String, Integer> getRecordCounts() {
        return recordCounts;
    }

    public void setRecordCounts(Map<String, Integer> recordCounts) {
        this.recordCounts = recordCounts;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
