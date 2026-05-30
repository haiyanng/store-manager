package com.customershopfx.profile.presenter;

import com.customershopfx.auth.model.Customer;
import com.customershopfx.profile.model.CustomerProfile;
import com.customershopfx.profile.service.ProfileApiService;
import com.customershopfx.profile.viewmodel.ProfileViewModel;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class ProfilePresenter {
    private final ProfileViewModel viewModel;
    private final ProfileApiService profileApiService;
    private final Consumer<CustomerProfile> checkoutSync;
    private final Consumer<Customer> headerSync;
    private final Consumer<String> statusSink;

    public ProfilePresenter(ProfileViewModel viewModel, Consumer<String> statusSink,
                            Consumer<CustomerProfile> checkoutSync, Consumer<Customer> headerSync) {
        this.viewModel = viewModel;
        this.statusSink = statusSink;
        this.checkoutSync = checkoutSync;
        this.headerSync = headerSync;
        this.profileApiService = new ProfileApiService();
    }

    public void initialize() {
        loadProfile();
    }

    public void loadProfile() {
        viewModel.markLoading("Loading profile...");
        statusSink.accept("Loading profile...");
        run(profileApiService::profile, profile -> {
            viewModel.setProfile(profile);
            checkoutSync.accept(profile);
            headerSync.accept(asCustomer(profile));
            viewModel.markSuccess("Ready");
            statusSink.accept("Ready");
        });
    }

    public void updateProfile() {
        viewModel.markLoading("Saving profile...");
        statusSink.accept("Saving profile...");
        run(() -> profileApiService.updateProfile(viewModel.getFullName(), viewModel.getPhone()), profile -> {
            viewModel.setProfile(profile);
            checkoutSync.accept(profile);
            headerSync.accept(asCustomer(profile));
            viewModel.markSuccess("Profile updated");
            statusSink.accept("Profile updated");
        });
    }

    public void changePassword() {
        viewModel.markLoading("Changing password...");
        statusSink.accept("Changing password...");
        run(() -> {
            profileApiService.changePassword(viewModel.getCurrentPassword(), viewModel.getNewPassword());
            return Boolean.TRUE;
        }, ok -> {
            viewModel.clearPasswords();
            viewModel.markSuccess("Password changed");
            statusSink.accept("Password changed");
        });
    }

    public ProfileViewModel viewModel() {
        return viewModel;
    }

    private Customer asCustomer(CustomerProfile profile) {
        return new Customer(profile.id(), profile.email(), profile.fullName(), profile.phone());
    }

    private <T> void run(Callable<T> callable, Consumer<T> success) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return callable.call();
            }
        };
        task.setOnSucceeded(event -> {
            try {
                success.accept(task.getValue());
            } catch (RuntimeException ex) {
                viewModel.markFailure(message(ex));
                statusSink.accept(message(ex));
            }
        });
        task.setOnFailed(event -> {
            String message = message(task.getException());
            viewModel.markFailure(message);
            statusSink.accept(message);
        });
        Thread thread = new Thread(task, "profile-api-task");
        thread.setDaemon(true);
        thread.start();
    }

    private String message(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && (current.getMessage() == null || current.getMessage().isBlank()) && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current == null ? null : current.getMessage();
        return message == null || message.isBlank() ? "Request failed" : message;
    }
}
