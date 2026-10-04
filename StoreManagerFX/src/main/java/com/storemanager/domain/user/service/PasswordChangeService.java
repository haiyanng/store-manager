package com.storemanager.domain.user.service;

import com.storemanager.core.session.AppSession;
import com.storemanager.core.security.PasswordHasher;
import com.storemanager.domain.user.repository.UserRepository;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.audit.service.AuditSnapshots;

public class PasswordChangeService {
    public void changePassword(String current, String replacement, String confirmation) {
        var actor = AppSession.getCurrentUser();
        if (actor == null) throw new IllegalStateException("Please sign in before changing your password.");
        validateNewPassword(replacement, confirmation);
        var repository = new UserRepository();
        var stored = repository.findById(actor.getId())
                .orElseThrow(() -> new IllegalStateException("Account no longer exists."));
        if (!stored.isActive()) throw new IllegalStateException("This account is inactive.");
        if (current == null || !PasswordHasher.hash(current).equals(stored.getPassword()))
            throw new IllegalArgumentException("Current password is incorrect.");
        String hash = PasswordHasher.hash(replacement);
        if (hash.equals(stored.getPassword()))
            throw new IllegalArgumentException("New password must differ from your current password.");
        if (!repository.changePassword(stored.getId(), stored.getPassword(), hash)) throw new IllegalStateException("Unable to save your new password. Please try again.");
        actor.setPassword(hash);
        new AuditService().recordChange("USER_MANAGEMENT", "USER_PASSWORD_CHANGE", "USER", actor.getId(),
                true, null, null, null, AuditSnapshots.fields("password_changed", true));
    }

    public static void validateNewPassword(String password, String confirmation) {
        if (password == null || password.isBlank()) throw new IllegalArgumentException("New password is required.");
        if (!password.equals(confirmation)) throw new IllegalArgumentException("New password and confirmation do not match.");
    }
}
