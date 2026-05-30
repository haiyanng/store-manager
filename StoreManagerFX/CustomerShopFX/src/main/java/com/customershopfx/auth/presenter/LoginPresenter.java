package com.customershopfx.auth.presenter;

import com.customershopfx.auth.model.AuthResponse;
import com.customershopfx.auth.service.AuthApiService;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class LoginPresenter {
    private final AuthApiService authApiService;

    public LoginPresenter() {
        this(new AuthApiService());
    }

    public LoginPresenter(AuthApiService authApiService) {
        this.authApiService = authApiService;
    }

    public void login(String email, String password, Consumer<AuthResponse> onSuccess, Consumer<String> onError) {
        runTask(() -> authApiService.login(email, password), onSuccess, onError);
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
        Thread thread = new Thread(task, "auth-login");
        thread.setDaemon(true);
        thread.start();
    }

    private String message(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        return message == null || message.isBlank() ? "Login failed" : message;
    }
}
