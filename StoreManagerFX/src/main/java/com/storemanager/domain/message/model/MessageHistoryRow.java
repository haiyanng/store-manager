package com.storemanager.domain.message.model;

import java.time.LocalDateTime;

public class MessageHistoryRow {

    private Long id;

    private String senderUsername;

    private String content;

    private boolean read;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(
            String senderUsername
    ) {
        this.senderUsername = senderUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(
            String content
    ) {
        this.content = content;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(
            boolean read
    ) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}
