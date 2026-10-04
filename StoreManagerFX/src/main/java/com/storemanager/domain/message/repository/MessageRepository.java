package com.storemanager.domain.message.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.message.model.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public boolean save(
            Message message
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO messages (
                                    sender_user_id,
                                    receiver_user_id,
                                    content,
                                    is_read,
                                    created_at
                                )
                                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                                """
                        )
        ) {

            statement.setObject(1, message.getSenderUserId());
            statement.setObject(2, message.getReceiverUserId());
            statement.setString(3, message.getContent());
            statement.setBoolean(4, message.isRead());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Message> findConversationMessages(
            Long userA,
            Long userB
    ) {

        List<Message> messages = new ArrayList<>();

        if (userA == null || userB == null) {
            return messages;
        }

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT *
                                FROM messages
                                WHERE (sender_user_id = ? AND receiver_user_id = ?)
                                   OR (sender_user_id = ? AND receiver_user_id = ?)
                                ORDER BY created_at ASC, id ASC
                                """
                        )
        ) {

            statement.setLong(1, userA);
            statement.setLong(2, userB);
            statement.setLong(3, userB);
            statement.setLong(4, userA);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(mapMessage(resultSet));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return messages;
    }

    public Message findLastMessageBetween(
            Long userA,
            Long userB
    ) {

        if (userA == null || userB == null) {
            return null;
        }

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT *
                                FROM messages
                                WHERE (sender_user_id = ? AND receiver_user_id = ?)
                                   OR (sender_user_id = ? AND receiver_user_id = ?)
                                ORDER BY created_at DESC, id DESC
                                LIMIT 1
                                """
                        )
        ) {

            statement.setLong(1, userA);
            statement.setLong(2, userB);
            statement.setLong(3, userB);
            statement.setLong(4, userA);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapMessage(resultSet);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public long countUnreadByReceiver(
            Long receiverUserId
    ) {

        if (receiverUserId == null) {
            return 0L;
        }

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT COUNT(*) AS unread_count
                                FROM messages
                                WHERE receiver_user_id = ?
                                  AND is_read = FALSE
                                """
                        )
        ) {

            statement.setLong(1, receiverUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("unread_count");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0L;
    }

    public long countUnreadBetween(
            Long receiverUserId,
            Long senderUserId
    ) {

        if (receiverUserId == null || senderUserId == null) {
            return 0L;
        }

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT COUNT(*) AS unread_count
                                FROM messages
                                WHERE receiver_user_id = ?
                                  AND sender_user_id = ?
                                  AND is_read = FALSE
                                """
                        )
        ) {

            statement.setLong(1, receiverUserId);
            statement.setLong(2, senderUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("unread_count");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0L;
    }

    public boolean markConversationAsRead(
            Long receiverUserId,
            Long senderUserId
    ) {

        if (receiverUserId == null || senderUserId == null) {
            return false;
        }

        try (
                Connection connection =
                        ConnectionFactory.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                UPDATE messages
                                SET is_read = TRUE
                                WHERE receiver_user_id = ?
                                  AND sender_user_id = ?
                                  AND is_read = FALSE
                                """
                        )
        ) {

            statement.setLong(1, receiverUserId);
            statement.setLong(2, senderUserId);

            return statement.executeUpdate() >= 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void initializeSchema(Connection connection) {

        try (
                Statement statement = connection.createStatement()
        ) {

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS messages (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        sender_user_id BIGINT NOT NULL,
                        receiver_user_id BIGINT NOT NULL,
                        content TEXT NOT NULL,
                        is_read BOOLEAN NOT NULL DEFAULT FALSE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Message table initialization failed",
                    e
            );
        }
    }

    private Message mapMessage(
            ResultSet resultSet
    ) throws Exception {

        Message message = new Message();
        message.setId(resultSet.getLong("id"));
        message.setSenderUserId(resultSet.getLong("sender_user_id"));
        message.setReceiverUserId(resultSet.getLong("receiver_user_id"));
        message.setContent(resultSet.getString("content"));
        message.setRead(resultSet.getBoolean("is_read"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            message.setCreatedAt(createdAt.toLocalDateTime());
        }

        return message;
    }
}
