package com.storemanager.domain.message.presenter;

import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.message.model.MessageConversationRow;
import com.storemanager.domain.message.service.MessageService;
import com.storemanager.domain.message.view.MessageInboxController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.List;

public class MessageInboxPresenter {

    private final MessageInboxController view;

    private final MessageService messageService =
            new MessageService();

    private LoadingState loadingState =
            LoadingState.IDLE;

    private Long selectedConversationUserId;

    private Timeline pollingTimeline;

    public MessageInboxPresenter(
            MessageInboxController view
    ) {

        this.view = view;
    }

    public void initialize() {

        refreshConversations();
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

    public void refreshConversations() {

        refreshConversations(true);
    }

    private void refreshConversations(
            boolean interactive
    ) {

        Long selectedConversationId =
                view.getSelectedConversationUserId();

        loadingState = LoadingState.LOADING;
        if (interactive) {
            view.setBusy(true);
            view.setStatus("Loading messages...");
        }

        AsyncTaskRunner.run(
                () -> messageService.findConversationRowsForCurrentUser(),
                rows -> {
                    view.setConversations(rows);
                    if (!interactive) {
                        view.selectConversationByUserId(selectedConversationId);
                    }
                    view.setUnreadCount(
                            messageService.countUnreadForCurrentUser()
                    );
                    loadingState = LoadingState.SUCCESS;
                    if (interactive) {
                        view.setStatus("Ready");
                    }
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    if (interactive) {
                        view.setStatus("Unable to load messages");
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

    public void selectConversation(
            MessageConversationRow conversation
    ) {

        if (conversation == null) {
            selectedConversationUserId = null;
            view.setMessages(List.of());
            view.clearMessageInput();
            view.setConversationLabel("No conversation selected");
            view.setSendEnabled(false);
            return;
        }

        selectedConversationUserId = conversation.getOtherUserId();
        view.setConversationLabel(
                conversation.getOtherUsername()
                        + " ("
                        + conversation.getOtherRole()
                        + ")"
        );
        view.setSendEnabled(true);
        refreshHistory(true);
    }

    public void sendMessage(
            String content
    ) {

        if (selectedConversationUserId == null) {
            view.showError("Select a conversation first");
            return;
        }

        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Sending message...");

        AsyncTaskRunner.run(
                () -> {
                    boolean success =
                            messageService.sendMessage(
                                    selectedConversationUserId,
                                    content
                            );

                    if (!success) {
                        throw new RuntimeException(
                                "Cannot send message"
                        );
                    }

                    return Boolean.TRUE;
                },
                result -> {
                    view.clearMessageInput();
                    refreshConversations(false);
                    loadingState = LoadingState.SUCCESS;
                    view.setStatus("Ready");
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Cannot send message");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void refreshHistory() {

        refreshHistory(true);
    }

    private void refreshHistory(
            boolean interactive
    ) {

        if (selectedConversationUserId == null) {
            view.setMessages(List.of());
            return;
        }

        AsyncTaskRunner.run(
                () -> messageService.findHistoryForConversation(
                        selectedConversationUserId
                ),
                rows -> view.setMessages(rows),
                throwable -> view.showError(throwable.getMessage()),
                () -> {
                }
        );
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private void refreshSilently() {

        refreshConversations(false);
    }
}
