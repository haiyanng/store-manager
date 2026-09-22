package com.storemanager.domain.auth.service;

import com.storemanager.core.security.PasswordHasher;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.repository.UserRepository;

import java.util.Optional;

public class AuthService {

    private final UserRepository userRepository =
            new UserRepository();

    private final AuditService auditService =
            new AuditService();

    public boolean login(
            String username,
            String rawPassword
    ) {

        Optional<User> optionalUser =
                userRepository.findByUsername(
                        username
                );

        if (optionalUser.isEmpty()) {
            auditService.recordLoginFailure(
                    username,
                    "User not found"
            );
            return false;
        }

        User user =
                optionalUser.get();

        String hashedPassword =
                PasswordHasher.hash(
                        rawPassword
                );

        boolean matched =
                hashedPassword.equals(
                        user.getPassword()
                );

        if (!matched) {
            auditService.recordLoginFailure(
                    username,
                    "Invalid password"
            );
            return false;
        }

        if (!user.isActive()) {
            auditService.recordLoginFailure(
                    username,
                    "Inactive user"
            );
            return false;
        }

        AppSession.setCurrentUser(user);

        auditService.recordLogin(user);

        return true;
    }

    public void logout() {

        User currentUser =
                AppSession.getCurrentUser();

        auditService.recordLogout(currentUser);

        AppSession.clear();
    }
}
