package com.storemanager.domain.message.service;

import com.storemanager.core.session.AppSession;
import com.storemanager.domain.message.model.Message;
import com.storemanager.domain.message.model.MessageConversationRow;
import com.storemanager.domain.message.model.MessageHistoryRow;
import com.storemanager.domain.message.repository.MessageRepository;
import com.storemanager.core.util.TimeFormatUtil;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MessageService {

    private final MessageRepository messageRepository =
            new MessageRepository();

    private final UserRepository userRepository =
            new UserRepository();

    public List<MessageConversationRow> findConversationRowsForCurrentUser() {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            return List.of();
        }

        List<User> users = userRepository.findAll();
        List<MessageConversationRow> rows = new ArrayList<>();

        for (User user : users) {
            if (user == null
                    || user.getId() == null
                    || user.getId().equals(currentUser.getId())) {
                continue;
            }

            Message lastMessage =
                    messageRepository.findLastMessageBetween(
                            currentUser.getId(),
                            user.getId()
                    );

            MessageConversationRow row =
                    new MessageConversationRow();

            row.setOtherUserId(user.getId());
            row.setOtherUsername(user.getUsername());
            row.setOtherRole(user.getRole() == null ? "" : user.getRole().name());
            row.setUnreadCount(
                    messageRepository.countUnreadBetween(
                            currentUser.getId(),
                            user.getId()
                    )
            );

            if (lastMessage != null) {
                row.setLastMessage(summarize(lastMessage.getContent()));
                row.setLastMessageAt(lastMessage.getCreatedAt());
            } else {
                row.setLastMessage("");
            }

            rows.add(row);
        }

        rows.sort(
                Comparator.comparing(
                                MessageConversationRow::getLastMessageAt,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .reversed()
                        .thenComparing(MessageConversationRow::getOtherUsername)
        );

        return rows;
    }

    public List<MessageHistoryRow> findHistoryForConversation(
            Long otherUserId
    ) {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null || otherUserId == null) {
            return List.of();
        }

        markConversationAsRead(otherUserId);

        List<Message> messages =
                messageRepository.findConversationMessages(
                        currentUser.getId(),
                        otherUserId
                );

        List<MessageHistoryRow> historyRows = new ArrayList<>();

        for (Message message : messages) {
            historyRows.add(mapHistoryRow(message));
        }

        return historyRows;
    }

    public boolean sendMessage(
            Long receiverUserId,
            String content
    ) {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User session is required");
        }

        if (receiverUserId == null) {
            throw new RuntimeException("Receiver is required");
        }

        if (currentUser.getId().equals(receiverUserId)) {
            throw new RuntimeException("Cannot send a message to yourself");
        }

        String cleanedContent = clean(content);
        if (cleanedContent == null) {
            throw new RuntimeException("Message content is required");
        }

        Optional<User> receiver =
                userRepository.findById(receiverUserId);

        if (receiver.isEmpty() || !receiver.get().isActive()) {
            throw new RuntimeException("Receiver is not available");
        }

        Message message = new Message();
        message.setSenderUserId(currentUser.getId());
        message.setReceiverUserId(receiverUserId);
        message.setContent(cleanedContent);
        message.setRead(false);

        return messageRepository.save(message);
    }

    public long countUnreadForCurrentUser() {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            return 0L;
        }

        return messageRepository.countUnreadByReceiver(currentUser.getId());
    }

    public long countUnreadForConversation(
            Long otherUserId
    ) {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null || otherUserId == null) {
            return 0L;
        }

        return messageRepository.countUnreadBetween(
                currentUser.getId(),
                otherUserId
        );
    }

    public String formatDateTime(
            java.time.LocalDateTime dateTime
    ) {

        return TimeFormatUtil.formatDateTime(dateTime);
    }

    private void markConversationAsRead(
            Long otherUserId
    ) {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null || otherUserId == null) {
            return;
        }

        messageRepository.markConversationAsRead(
                currentUser.getId(),
                otherUserId
        );
    }

    private MessageHistoryRow mapHistoryRow(
            Message message
    ) {

        MessageHistoryRow row = new MessageHistoryRow();
        row.setId(message.getId());
        row.setSenderUsername(
                userRepository.findById(
                        message.getSenderUserId()
                ).map(User::getUsername).orElse("Unknown")
        );
        row.setContent(message.getContent());
        row.setRead(message.isRead());
        row.setCreatedAt(message.getCreatedAt());
        return row;
    }

    private String summarize(
            String content
    ) {

        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        String cleaned = content.trim();
        if (cleaned.length() <= 80) {
            return cleaned;
        }

        return cleaned.substring(0, 77) + "...";
    }

    private String clean(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
