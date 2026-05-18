package com.storemanager.domain.auth.dto;

import com.storemanager.domain.license.model.LicenseStatus;

public record LoginResponse(

        boolean success,

        String message,

        LicenseStatus licenseStatus

) {

    public static LoginResponse success(
            LicenseStatus licenseStatus
    ) {

        return new LoginResponse(
                true,
                "Login successful",
                licenseStatus
        );
    }

    public static LoginResponse invalidCredentials(
            LicenseStatus licenseStatus
    ) {

        return new LoginResponse(
                false,
                "Sai tài khoản hoặc mật khẩu",
                licenseStatus
        );
    }

    public static LoginResponse blocked(
            LicenseStatus licenseStatus
    ) {

        return new LoginResponse(
                false,
                licenseStatus.message(),
                licenseStatus
        );
    }
}
