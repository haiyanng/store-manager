package com.storemanager.domain.message.model;

import java.time.LocalDateTime;

public class Message {

    private Long id;

    private Long senderUserId;

    private Long receiverUserId;

    private String content;

    private boolean read;

    private LocalDateTime createdAt;

    public Message() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(
            Long senderUserId
    ) {
        this.senderUserId = senderUserId;
    }

    public Long getReceiverUserId() {
        return receiverUserId;
    }

    public void setReceiverUserId(
            Long receiverUserId
    ) {
        this.receiverUserId = receiverUserId;
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
