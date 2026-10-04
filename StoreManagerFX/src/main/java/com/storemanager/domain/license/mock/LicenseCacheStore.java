package com.storemanager.domain.license.mock;

import com.storemanager.domain.license.model.LicenseDevice;
import com.storemanager.domain.license.model.LicenseState;
import com.storemanager.domain.license.model.LicenseStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

final class LicenseCacheStore {

    private static final Path CACHE_FILE =
            Path.of(
                    System.getProperty("user.home"),
                    ".storemanagerfx",
                    "license",
                    "license-cache.properties"
            );

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    Optional<LicenseStatus> load() {

        if (!Files.exists(CACHE_FILE)) {
            return Optional.empty();
        }

        Properties properties =
                new Properties();

        try (InputStream inputStream =
                     Files.newInputStream(CACHE_FILE)) {

            properties.load(
                    inputStream
            );

            return Optional.of(
                    readStatus(properties)
            );

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    void save(
            LicenseStatus status
    ) throws IOException {

        Files.createDirectories(
                CACHE_FILE.getParent()
        );

        Properties properties =
                new Properties();

        properties.setProperty(
                "state",
                status.state().name()
        );
        properties.setProperty(
                "message",
                status.message()
        );
        properties.setProperty(
                "loginAllowed",
                Boolean.toString(
                        status.loginAllowed()
                )
        );
        properties.setProperty(
                "allowedSlots",
                Integer.toString(
                        status.allowedSlots()
                )
        );
        properties.setProperty(
                "currentInstallationId",
                safeValue(
                        status.currentInstallationId()
                )
        );
        properties.setProperty(
                "nextVerificationAt",
                formatDateTime(
                        status.nextVerificationAt()
                )
        );
        properties.setProperty(
                "graceUntil",
                formatDateTime(
                        status.graceUntil()
                )
        );
        properties.setProperty(
                "lastVerifiedAt",
                formatDateTime(
                        status.lastVerifiedAt()
                )
        );
        properties.setProperty(
                "deviceCount",
                Integer.toString(
                        status.activatedDevices().size()
                )
        );

        for (int index = 0;
             index < status.activatedDevices().size();
             index++) {

            LicenseDevice device =
                    status.activatedDevices().get(
                            index
                    );

            properties.setProperty(
                    deviceKey(
                            index,
                            "installationId"
                    ),
                    safeValue(
                            device.installationId()
                    )
            );
            properties.setProperty(
                    deviceKey(
                            index,
                            "deviceName"
                    ),
                    safeValue(
                            device.deviceName()
                    )
            );
            properties.setProperty(
                    deviceKey(
                            index,
                            "activatedAt"
                    ),
                    formatDateTime(
                            device.activatedAt()
                    )
            );
            properties.setProperty(
                    deviceKey(
                            index,
                            "lastVerifiedAt"
                    ),
                    formatDateTime(
                            device.lastVerifiedAt()
                    )
            );
            properties.setProperty(
                    deviceKey(
                            index,
                            "active"
                    ),
                    Boolean.toString(
                            device.active()
                    )
            );
        }

        try (OutputStream outputStream =
                     Files.newOutputStream(CACHE_FILE)) {

            properties.store(
                    outputStream,
                    "StoreManagerFX license cache"
            );
        }
    }

    private LicenseStatus readStatus(
            Properties properties
    ) {

        LicenseState state =
                LicenseState.valueOf(
                        properties.getProperty(
                                "state",
                                LicenseState.ACTIVE.name()
                        )
                );

        String message =
                properties.getProperty(
                        "message",
                        defaultMessage(state)
                );

        boolean loginAllowed =
                Boolean.parseBoolean(
                        properties.getProperty(
                                "loginAllowed",
                                Boolean.toString(
                                        state.isLoginAllowed()
                                )
                        )
                );

        int allowedSlots =
                Integer.parseInt(
                        properties.getProperty(
                                "allowedSlots",
                                "1"
                        )
                );

        String currentInstallationId =
                properties.getProperty(
                        "currentInstallationId",
                        ""
                );

        LocalDateTime nextVerificationAt =
                parseDateTime(
                        properties.getProperty(
                                "nextVerificationAt"
                        )
                );

        LocalDateTime graceUntil =
                parseDateTime(
                        properties.getProperty(
                                "graceUntil"
                        )
                );

        LocalDateTime lastVerifiedAt =
                parseDateTime(
                        properties.getProperty(
                                "lastVerifiedAt"
                        )
                );

        int deviceCount =
                Integer.parseInt(
                        properties.getProperty(
                                "deviceCount",
                                "0"
                        )
                );

        List<LicenseDevice> devices =
                new ArrayList<>();

        for (int index = 0; index < deviceCount; index++) {

            devices.add(
                    new LicenseDevice(
                            properties.getProperty(
                                    deviceKey(
                                            index,
                                            "installationId"
                                    ),
                                    ""
                            ),
                            properties.getProperty(
                                    deviceKey(
                                            index,
                                            "deviceName"
                                    ),
                                    ""
                            ),
                            parseDateTime(
                                    properties.getProperty(
                                            deviceKey(
                                                    index,
                                                    "activatedAt"
                                            )
                                    )
                            ),
                            parseDateTime(
                                    properties.getProperty(
                                            deviceKey(
                                                    index,
                                                    "lastVerifiedAt"
                                            )
                                    )
                            ),
                            Boolean.parseBoolean(
                                    properties.getProperty(
                                            deviceKey(
                                                    index,
                                                    "active"
                                            ),
                                            "false"
                                    )
                            )
                    )
            );
        }

        return new LicenseStatus(
                state,
                message,
                loginAllowed,
                allowedSlots,
                devices,
                currentInstallationId,
                nextVerificationAt,
                graceUntil,
                lastVerifiedAt
        );
    }

    private static String deviceKey(
            int index,
            String suffix
    ) {

        return "device."
                + index
                + "."
                + suffix;
    }

    private static String formatDateTime(
            LocalDateTime dateTime
    ) {

        return dateTime == null
                ? ""
                : FORMATTER.format(dateTime);
    }

    private static LocalDateTime parseDateTime(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return LocalDateTime.parse(
                value.trim(),
                FORMATTER
        );
    }

    private static String safeValue(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }

    private static String defaultMessage(
            LicenseState state
    ) {

        return switch (state) {
            case ACTIVE ->
                    "License verified. 1 device slot active.";
            case GRACE_PERIOD ->
                    "License verification failed. Grace period active.";
            case EXPIRED ->
                    "License expired. Login blocked.";
            case DEVICE_LIMIT_EXCEEDED ->
                    "Device limit exceeded. Owner action required.";
        };
    }
}
