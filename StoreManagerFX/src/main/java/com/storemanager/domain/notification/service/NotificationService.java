package com.storemanager.domain.notification.service;

import com.storemanager.core.session.AppSession;
import com.storemanager.domain.notification.model.Notification;
import com.storemanager.domain.notification.model.NotificationType;
import com.storemanager.domain.notification.repository.NotificationRepository;
import com.storemanager.domain.user.model.User;

import java.util.List;

public class NotificationService {

    private final NotificationRepository notificationRepository =
            new NotificationRepository();

    public boolean createNotification(
            Long userId,
            String title,
            String content,
            NotificationType type
    ) {

        return createNotification(userId, title, content, type, false);
    }

    public boolean createNotification(
            Long userId,
            String title,
            String content,
            NotificationType type,
            boolean read
    ) {

        if (userId == null) {
            return false;
        }

        if (type == null) {
            return false;
        }

        Notification notification =
                new Notification();

        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setRead(read);

        return notificationRepository.save(notification);
    }

    public boolean notifyCurrentUser(
            String title,
            String content,
            NotificationType type
    ) {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        return createNotification(
                currentUser.getId(),
                title,
                content,
                type
        );
    }

    public boolean notifyUser(
            Long userId,
            String title,
            String content,
            NotificationType type
    ) {

        return createNotification(
                userId,
                title,
                content,
                type
        );
    }

    public List<Notification> findNotificationsForCurrentUser() {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            return List.of();
        }

        return notificationRepository.findByUserId(currentUser.getId());
    }

    public List<Notification> findRecentNotificationsForCurrentUser(
            int limit
    ) {

        if (limit <= 0) {
            return List.of();
        }

        List<Notification> notifications =
                findNotificationsForCurrentUser();

        if (notifications.size() <= limit) {
            return notifications;
        }

        return notifications.subList(0, limit);
    }

    public long countUnreadForCurrentUser() {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            return 0L;
        }

        return notificationRepository.countUnreadByUserId(
                currentUser.getId()
        );
    }

    public boolean markAsRead(
            Long notificationId
    ) {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null || notificationId == null) {
            return false;
        }

        return notificationRepository.markAsRead(
                notificationId,
                currentUser.getId()
        );
    }
}
