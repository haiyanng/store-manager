package com.storemanager.domain.auth.presenter;

import com.storemanager.domain.auth.dto.LoginResponse;
import com.storemanager.domain.auth.service.AuthService;
import com.storemanager.domain.license.mock.LicenseServiceMock;
import com.storemanager.domain.license.model.LicenseStatus;

public class LoginPresenter {

    private final AuthService authService =
            new AuthService();

    private final LicenseServiceMock licenseService =
            new LicenseServiceMock();

    public LicenseStatus initialize() {

        return licenseService.refreshOnStartup();
    }

    public LicenseStatus getLicenseStatus() {

        return licenseService.getCurrentStatus();
    }

    public LoginResponse login(
            String username,
            String password
    ) {

        LicenseStatus licenseStatus =
                licenseService.getCurrentStatus();

        if (!licenseStatus.loginAllowed()) {
            return LoginResponse.blocked(
                    licenseStatus
            );
        }

        boolean success =
                authService.login(
                        username,
                        password
                );

        if (!success) {
            return LoginResponse.invalidCredentials(
                    licenseStatus
            );
        }

        return LoginResponse.success(
                licenseStatus
        );
    }
}
