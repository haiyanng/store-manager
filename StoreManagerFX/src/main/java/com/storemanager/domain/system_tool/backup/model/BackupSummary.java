package com.storemanager.domain.system_tool.backup.model;

import java.time.LocalDateTime;

public class BackupSummary {

    private LocalDateTime createdAt;
    private long sizeBytes;
    private String location;

    public BackupSummary() {
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
