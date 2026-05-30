package com.customershopfx.auth.presenter;

import com.customershopfx.auth.model.AuthResponse;
import com.customershopfx.auth.service.AuthApiService;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class RegisterPresenter {
    private final AuthApiService authApiService;

    public RegisterPresenter() {
        this(new AuthApiService());
    }

    public RegisterPresenter(AuthApiService authApiService) {
        this.authApiService = authApiService;
    }

    public void register(String email, String password, String fullName, String phone,
                         Consumer<AuthResponse> onSuccess, Consumer<String> onError) {
        runTask(() -> authApiService.register(email, password, fullName, phone), onSuccess, onError);
    }

    private <T> void runTask(Callable<T> callable, Consumer<T> onSuccess, Consumer<String> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return callable.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onError.accept(message(task.getException())));
        Thread thread = new Thread(task, "auth-register");
        thread.setDaemon(true);
        thread.start();
    }

    private String message(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        return message == null || message.isBlank() ? "Register failed" : message;
    }
}
