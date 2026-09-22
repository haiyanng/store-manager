package com.storemanager.domain.system_tool.migration_export.model;

import java.time.LocalDateTime;

public class MigrationHistoryEntry {

    private LocalDateTime date;
    private MigrationDirection direction;
    private MigrationStatusLevel status;
    private int recordCount;
    private String user;

    public MigrationHistoryEntry() {
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public MigrationDirection getDirection() {
        return direction;
    }

    public void setDirection(MigrationDirection direction) {
        this.direction = direction;
    }

    public MigrationStatusLevel getStatus() {
        return status;
    }

    public void setStatus(MigrationStatusLevel status) {
        this.status = status;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
