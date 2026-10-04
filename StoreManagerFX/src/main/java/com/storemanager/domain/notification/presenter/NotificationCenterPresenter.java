package com.storemanager.domain.notification.presenter;

import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.notification.model.Notification;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.notification.view.NotificationCenterController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Callable;

public class NotificationCenterPresenter {

    private final NotificationCenterController view;
    private final NotificationService notificationService = new NotificationService();
    private LoadingState loadingState = LoadingState.IDLE;
    private Long selectedNotificationId;
    private Timeline pollingTimeline;
    private boolean busy;

    public NotificationCenterPresenter(NotificationCenterController view) {
        this.view = view;
    }

    public void initialize() {
        refreshNotifications();
    }

    public void startPolling() {
        if (pollingTimeline != null) return;
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(15), event -> refreshNotifications(false)));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    public void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            pollingTimeline = null;
        }
    }

    public void refreshNotifications() {
        refreshNotifications(true);
    }

    private void refreshNotifications(boolean interactive) {
        runTask(this::loadNotifications, "Loading notifications...", "Unable to load notifications", interactive);
    }

    public void selectNotification(Notification notification) {
        selectedNotificationId = notification == null ? null : notification.getId();
        view.setMarkAsReadEnabled(notification != null && !notification.isRead());
    }

    public void markSelectedAsRead() {
        if (busy) return;
        if (selectedNotificationId == null) {
            view.showError("Select a notification first");
            return;
        }
        Long notificationId = selectedNotificationId;
        busy = true;
        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Marking notification as read...");
        boolean[] saved = {false};
        AsyncTaskRunner.run(() -> notificationService.markAsRead(notificationId),
                success -> {
                    if (!success) {
                        loadingState = LoadingState.ERROR;
                        view.setStatus("Cannot update notification");
                        view.showError("Cannot mark notification as read");
                        return;
                    }
                    saved[0] = true;
                    view.setMarkAsReadEnabled(false);
                }, error -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Cannot update notification");
                    view.showError(error.getMessage());
                }, () -> {
                    if (saved[0]) {
                        busy = false;
                        runTask(this::loadNotifications, "Refreshing notifications...",
                                "Notification updated; unable to refresh. Click Refresh to reload.", true);
                    } else {
                        finishTask();
                    }
                });
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private List<Notification> loadNotifications() {
        return notificationService.findNotificationsForCurrentUser();
    }

    private void runTask(Callable<List<Notification>> task, String status, String failureStatus, boolean interactive) {
        if (busy) return;
        Long selection = view.getSelectedNotificationId();
        busy = true;
        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        if (interactive) view.setStatus(status);
        AsyncTaskRunner.run(task,
                notifications -> {
                    view.setNotifications(notifications);
                    view.selectNotificationById(selection);
                    view.setRecentNotifications(notifications.stream().limit(5).toList());
                    view.setUnreadCount(notifications.stream().filter(notification -> !notification.isRead()).count());
                    loadingState = LoadingState.SUCCESS;
                    if (interactive) view.setStatus("Ready");
                }, error -> {
                    loadingState = LoadingState.ERROR;
                    if (interactive) {
                        view.setStatus(failureStatus);
                        view.showError(error.getMessage());
                    }
                }, this::finishTask);
    }

    private void finishTask() {
        busy = false;
        view.setBusy(false);
    }
}
