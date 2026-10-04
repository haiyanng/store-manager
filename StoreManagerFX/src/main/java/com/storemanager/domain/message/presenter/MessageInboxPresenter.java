package com.storemanager.domain.message.presenter;

import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.message.model.MessageConversationRow;
import com.storemanager.domain.message.model.MessageHistoryRow;
import com.storemanager.domain.message.service.MessageService;
import com.storemanager.domain.message.view.MessageInboxController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Callable;

public class MessageInboxPresenter {

    private final MessageInboxController view;
    private final MessageService messageService = new MessageService();
    private LoadingState loadingState = LoadingState.IDLE;
    private Long selectedConversationUserId;
    private Timeline pollingTimeline;
    private boolean busy;
    private boolean applyingData;

    public MessageInboxPresenter(MessageInboxController view) {
        this.view = view;
    }

    public void initialize() {
        refreshConversations();
    }

    public void startPolling() {
        if (pollingTimeline != null) return;
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(15), event -> refreshConversations(false)));
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

    private void refreshConversations(boolean interactive) {
        Long conversationId = selectedConversationUserId;
        runTask(() -> loadData(conversationId), "Loading messages...", "Unable to load messages", interactive);
    }

    public void selectConversation(MessageConversationRow conversation) {
        if (applyingData || busy) return;
        selectedConversationUserId = conversation == null ? null : conversation.getOtherUserId();
        updateConversation(conversation);
        view.setMessages(List.of());
        view.clearMessageInput();
        if (conversation != null) refreshHistory();
    }

    public void sendMessage(String content) {
        if (busy) return;
        if (selectedConversationUserId == null) {
            view.showError("Select a conversation first");
            return;
        }
        Long receiverId = selectedConversationUserId;
        busy = true;
        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Sending message...");
        boolean[] sent = {false};
        AsyncTaskRunner.run(() -> messageService.sendMessage(receiverId, content),
                success -> {
                    if (!success) {
                        loadingState = LoadingState.ERROR;
                        view.setStatus("Cannot send message");
                        view.showError("Cannot send message");
                        return;
                    }
                    sent[0] = true;
                    view.clearMessageInput();
                }, error -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Cannot send message");
                    view.showError(error.getMessage());
                }, () -> {
                    if (sent[0]) {
                        busy = false;
                        runTask(() -> loadData(receiverId), "Refreshing messages...",
                                "Message sent; unable to refresh. Click Refresh to reload.", true);
                    } else {
                        finishTask();
                    }
                });
    }

    public void refreshHistory() {
        if (selectedConversationUserId == null) {
            view.setMessages(List.of());
            return;
        }
        Long conversationId = selectedConversationUserId;
        runTask(() -> loadData(conversationId), "Loading conversation...", "Unable to load conversation", true);
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private MessageData loadData(Long conversationId) {
        List<MessageHistoryRow> history = conversationId == null
                ? List.of() : messageService.findHistoryForConversation(conversationId);
        return new MessageData(conversationId, messageService.findConversationRowsForCurrentUser(),
                history, messageService.countUnreadForCurrentUser());
    }

    private void applyData(MessageData data) {
        applyingData = true;
        try {
            MessageConversationRow selected = data.conversations().stream()
                    .filter(row -> row.getOtherUserId().equals(data.conversationId()))
                    .findFirst().orElse(null);
            view.setConversations(data.conversations());
            selectedConversationUserId = selected == null ? null : selected.getOtherUserId();
            view.selectConversationByUserId(selectedConversationUserId);
            updateConversation(selected);
            view.setMessages(selected == null ? List.of() : data.history());
            view.setUnreadCount(data.unreadCount());
        } finally {
            applyingData = false;
        }
    }

    private void updateConversation(MessageConversationRow conversation) {
        if (conversation == null) {
            view.setConversationLabel("No conversation selected");
            view.setSendEnabled(false);
            return;
        }
        view.setConversationLabel(conversation.getOtherUsername() + " (" + conversation.getOtherRole() + ")");
        view.setSendEnabled(true);
    }

    private void runTask(Callable<MessageData> task, String status, String failureStatus, boolean interactive) {
        if (busy) return;
        busy = true;
        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        if (interactive) view.setStatus(status);
        AsyncTaskRunner.run(task,
                data -> {
                    applyData(data);
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

    private record MessageData(Long conversationId, List<MessageConversationRow> conversations,
                               List<MessageHistoryRow> history, long unreadCount) {
    }
}
