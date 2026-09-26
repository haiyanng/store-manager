package com.storemanager.domain.notification.presenter;

import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.notification.model.Notification;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.notification.view.NotificationCenterController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;


public class NotificationCenterPresenter {

    private final NotificationCenterController view;

    private final NotificationService notificationService =
            new NotificationService();

    private LoadingState loadingState =
            LoadingState.IDLE;

    private Long selectedNotificationId;

    private Timeline pollingTimeline;

    public NotificationCenterPresenter(
            NotificationCenterController view
    ) {

        this.view = view;
    }

    public void initialize() {

        refreshNotifications();
    }

    public void startPolling() {

        if (pollingTimeline != null) {
            return;
        }

        pollingTimeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(15),
                        event -> refreshSilently()
                )
        );
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

    private void refreshNotifications(
            boolean interactive
    ) {

        Long selectedNotificationId =
                view.getSelectedNotificationId();

        loadingState = LoadingState.LOADING;
        if (interactive) {
            view.setBusy(true);
            view.setStatus("Loading notifications...");
        }

        AsyncTaskRunner.run(
                () -> notificationService.findNotificationsForCurrentUser(),
                notifications -> {
                    view.setNotifications(notifications);
                    if (!interactive) {
                        view.selectNotificationById(selectedNotificationId);
                    }
                    view.setRecentNotifications(
                            notificationService.findRecentNotificationsForCurrentUser(5)
                    );
                    view.setUnreadCount(
                            notificationService.countUnreadForCurrentUser()
                    );
                    loadingState = LoadingState.SUCCESS;
                    if (interactive) {
                        view.setStatus("Ready");
                    }
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    if (interactive) {
                        view.setStatus("Unable to load notifications");
                        view.showError(throwable.getMessage());
                    }
                },
                () -> {
                    if (interactive) {
                        view.setBusy(false);
                    }
                }
        );
    }

    public void selectNotification(
            Notification notification
    ) {

        if (notification == null) {
            selectedNotificationId = null;
            view.setMarkAsReadEnabled(false);
            return;
        }

        selectedNotificationId = notification.getId();
        view.setMarkAsReadEnabled(!notification.isRead());
    }

    public void markSelectedAsRead() {

        if (selectedNotificationId == null) {
            view.showError("Select a notification first");
            return;
        }

        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Marking notification as read...");

        AsyncTaskRunner.run(
                () -> {
                    boolean success =
                            notificationService.markAsRead(
                                    selectedNotificationId
                            );

                    if (!success) {
                        throw new RuntimeException(
                                "Cannot mark notification as read"
                        );
                    }

                    return Boolean.TRUE;
                },
                result -> {
                    refreshNotifications(false);
                    loadingState = LoadingState.SUCCESS;
                    view.setStatus("Ready");
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Cannot update notification");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private void refreshSilently() {

        refreshNotifications(false);
    }
}
