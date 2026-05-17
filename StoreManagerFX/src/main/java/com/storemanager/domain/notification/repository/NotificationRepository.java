package com.storemanager.domain.notification.repository;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.domain.notification.model.Notification;
import com.storemanager.domain.notification.model.NotificationType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    public NotificationRepository() {
        initializeTable();
    }

    public boolean save(
            Notification notification
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO notifications (
                                    user_id,
                                    title,
                                    content,
                                    type,
                                    is_read,
                                    created_at
                                )
                                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                                """
                        )
        ) {

            statement.setObject(1, notification.getUserId());
            statement.setString(2, notification.getTitle());
            statement.setString(3, notification.getContent());
            statement.setString(4, notification.getType().name());
            statement.setBoolean(5, notification.isRead());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }

    public List<Notification> findByUserId(
            Long userId
    ) {

        List<Notification> notifications =
                new ArrayList<>();

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT *
                                FROM notifications
                                WHERE user_id = ?
                                ORDER BY created_at DESC, id DESC
                                """
                        )
        ) {

            statement.setLong(1, userId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                while (resultSet.next()) {
                    notifications.add(mapNotification(resultSet));
                }
            }

            return notifications;

        } catch (Exception e) {

            e.printStackTrace();
            return notifications;
        }
    }

    public long countUnreadByUserId(
            Long userId
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT COUNT(*) AS unread_count
                                FROM notifications
                                WHERE user_id = ?
                                  AND is_read = FALSE
                                """
                        )
        ) {

            statement.setLong(1, userId);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("unread_count");
            }

            return 0L;

        } catch (Exception e) {

            e.printStackTrace();
            return 0L;
        }
    }

    public boolean markAsRead(
            Long notificationId,
            Long userId
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                UPDATE notifications
                                SET is_read = TRUE
                                WHERE id = ?
                                  AND user_id = ?
                                """
                        )
        ) {

            statement.setLong(1, notificationId);
            statement.setLong(2, userId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }

    private void initializeTable() {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS notifications (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        user_id BIGINT NOT NULL,
                        title VARCHAR(160) NOT NULL,
                        content TEXT,
                        type VARCHAR(40) NOT NULL,
                        is_read BOOLEAN NOT NULL DEFAULT FALSE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
            );

        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException(
                    "Notification table initialization failed",
                    e
            );
        }
    }

    private Notification mapNotification(
            ResultSet resultSet
    ) throws Exception {

        Notification notification =
                new Notification();

        notification.setId(resultSet.getLong("id"));
        notification.setUserId(resultSet.getLong("user_id"));
        notification.setTitle(resultSet.getString("title"));
        notification.setContent(resultSet.getString("content"));
        notification.setType(NotificationType.valueOf(resultSet.getString("type")));
        notification.setRead(resultSet.getBoolean("is_read"));

        Timestamp createdAt =
                resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }

        return notification;
    }
}
