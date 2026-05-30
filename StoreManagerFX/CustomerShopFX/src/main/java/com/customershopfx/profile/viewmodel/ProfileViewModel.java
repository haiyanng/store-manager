package com.customershopfx.profile.viewmodel;

import com.customershopfx.profile.model.CustomerProfile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProfileViewModel {
    private final StringProperty email = new SimpleStringProperty("");
    private final StringProperty fullName = new SimpleStringProperty("");
    private final StringProperty phone = new SimpleStringProperty("");
    private final StringProperty currentPassword = new SimpleStringProperty("");
    private final StringProperty newPassword = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final StringProperty successMessage = new SimpleStringProperty("");

    public String getEmail() {
        return email.get();
    }

    public void setEmail(String value) {
        email.set(value == null ? "" : value);
    }

    public StringProperty emailProperty() {
        return email;
    }

    public String getFullName() {
        return fullName.get();
    }

    public void setFullName(String value) {
        fullName.set(value == null ? "" : value);
    }

    public StringProperty fullNameProperty() {
        return fullName;
    }

    public String getPhone() {
        return phone.get();
    }

    public void setPhone(String value) {
        phone.set(value == null ? "" : value);
    }

    public StringProperty phoneProperty() {
        return phone;
    }

    public String getCurrentPassword() {
        return currentPassword.get();
    }

    public void setCurrentPassword(String value) {
        currentPassword.set(value == null ? "" : value);
    }

    public StringProperty currentPasswordProperty() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword.get();
    }

    public void setNewPassword(String value) {
        newPassword.set(value == null ? "" : value);
    }

    public StringProperty newPasswordProperty() {
        return newPassword;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public void setLoading(boolean value) {
        loading.set(value);
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public void setErrorMessage(String value) {
        errorMessage.set(value == null ? "" : value);
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public String getSuccessMessage() {
        return successMessage.get();
    }

    public void setSuccessMessage(String value) {
        successMessage.set(value == null ? "" : value);
    }

    public StringProperty successMessageProperty() {
        return successMessage;
    }

    public void setProfile(CustomerProfile profile) {
        if (profile == null) {
            setEmail("");
            setFullName("");
            setPhone("");
            return;
        }
        setEmail(profile.email());
        setFullName(profile.fullName());
        setPhone(profile.phone());
    }

    public CustomerProfile toProfile(Long id) {
        return new CustomerProfile(id, getEmail(), getFullName(), getPhone());
    }

    public void clearPasswords() {
        setCurrentPassword("");
        setNewPassword("");
    }

    public void markLoading(String message) {
        setLoading(true);
        setErrorMessage("");
        setSuccessMessage(message);
    }

    public void markSuccess(String message) {
        setLoading(false);
        setErrorMessage("");
        setSuccessMessage(message);
    }

    public void markFailure(String message) {
        setLoading(false);
        setErrorMessage(message);
    }
}
