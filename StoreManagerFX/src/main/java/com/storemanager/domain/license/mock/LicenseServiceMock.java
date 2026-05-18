package com.storemanager.domain.license.mock;

import com.storemanager.domain.license.model.LicenseDevice;
import com.storemanager.domain.license.model.LicenseState;
import com.storemanager.domain.license.model.LicenseStatus;
import com.storemanager.domain.license.model.LicenseVerificationMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LicenseServiceMock {

    private static final int VERIFICATION_INTERVAL_DAYS = 14;

    private static final int DEFAULT_GRACE_DAYS = 5;

    private static final int DEFAULT_ALLOWED_SLOTS = 1;

    private static final Path INSTALLATION_ID_FILE =
            Path.of(
                    System.getProperty("user.home"),
                    ".storemanagerfx",
                    "license",
                    "installation-id.txt"
            );

    private final LicenseCacheStore cacheStore =
            new LicenseCacheStore();

    private final String installationId =
            loadOrCreateInstallationId();

    private final String deviceName =
            resolveDeviceName();

    private LicenseVerificationMode simulationMode =
            resolveSimulationMode();

    public LicenseStatus refreshOnStartup() {

        LicenseStatus cachedStatus =
                loadCachedStatus();

        LicenseStatus refreshedStatus =
                refresh(cachedStatus);

        saveQuietly(
                refreshedStatus
        );

        return refreshedStatus;
    }

    public LicenseStatus getCurrentStatus() {

        LicenseStatus status =
                loadCachedStatus();

        LicenseStatus normalizedStatus =
                normalize(
                status
        );

        if (!normalizedStatus.equals(
                status
        )) {
            saveQuietly(
                    normalizedStatus
            );
        }

        return normalizedStatus;
    }

    public LicenseStatus mockSuccessfulVerification() {

        simulationMode =
                LicenseVerificationMode.SUCCESS;

        return refreshOnStartup();
    }

    public LicenseStatus mockExpiredLicense() {

        simulationMode =
                LicenseVerificationMode.EXPIRED;

        return refreshOnStartup();
    }

    public LicenseStatus mockGracePeriod() {

        simulationMode =
                LicenseVerificationMode.GRACE_PERIOD;

        return refreshOnStartup();
    }

    public LicenseStatus mockDeviceLimitExceeded() {

        simulationMode =
                LicenseVerificationMode.DEVICE_LIMIT_EXCEEDED;

        return refreshOnStartup();
    }

    public LicenseStatus activateCurrentDevice() {

        LicenseStatus currentStatus =
                normalize(
                        loadCachedStatus()
                );

        List<LicenseDevice> devices =
                new ArrayList<>(
                        currentStatus.activatedDevices()
                );

        boolean currentDeviceActive =
                isCurrentDeviceActive(
                        devices
                );

        if (!currentDeviceActive
                && activeDeviceCount(devices) >= currentStatus.allowedSlots()) {
            return deviceLimitExceededStatus(
                    currentStatus
            );
        }

        List<LicenseDevice> activatedDevices =
                activateCurrentDevice(
                        devices
                );

        LicenseStatus updatedStatus =
                activeStatus(
                        currentStatus.allowedSlots(),
                        activatedDevices,
                        "Current device activated."
                );

        saveQuietly(
                updatedStatus
        );

        return updatedStatus;
    }

    public LicenseStatus deactivateDevice(
            String targetInstallationId
    ) {

        LicenseStatus currentStatus =
                normalize(
                        loadCachedStatus()
                );

        List<LicenseDevice> devices =
                new ArrayList<>();

        for (LicenseDevice device : currentStatus.activatedDevices()) {
            if (!device.installationId().equals(
                    targetInstallationId
            )) {
                devices.add(
                        device
                );
            }
        }

        LicenseStatus updatedStatus =
                statusFromDevices(
                        currentStatus.allowedSlots(),
                        LicenseState.ACTIVE,
                        "Device deactivated.",
                        devices,
                        LocalDateTime.now(),
                        null
                );

        saveQuietly(
                updatedStatus
        );

        return updatedStatus;
    }

    public LicenseStatus replaceDevice(
            String targetInstallationId
    ) {

        LicenseStatus afterDeactivate =
                deactivateDevice(
                        targetInstallationId
                );

        return activateCurrentDevice(
                afterDeactivate
        );
    }

    private LicenseStatus activateCurrentDevice(
            LicenseStatus baseStatus
    ) {

        List<LicenseDevice> devices =
                new ArrayList<>(
                        baseStatus.activatedDevices()
                );

        boolean currentDeviceActive =
                isCurrentDeviceActive(
                        devices
                );

        if (!currentDeviceActive
                && activeDeviceCount(devices) >= baseStatus.allowedSlots()) {
            return deviceLimitExceededStatus(
                    baseStatus
            );
        }

        List<LicenseDevice> activatedDevices =
                activateCurrentDevice(
                        devices
                );

        LicenseStatus updatedStatus =
                activeStatus(
                        baseStatus.allowedSlots(),
                        activatedDevices,
                        "Device replacement confirmed."
                );

        saveQuietly(
                updatedStatus
        );

        return updatedStatus;
    }

    private LicenseStatus refresh(
            LicenseStatus currentStatus
    ) {

        LicenseStatus normalizedStatus =
                normalize(
                        currentStatus
                );

        if (normalizedStatus.state() == LicenseState.DEVICE_LIMIT_EXCEEDED) {
            return normalizedStatus;
        }

        return switch (simulationMode) {
            case SUCCESS -> activeStatus(
                    normalizedStatus.allowedSlots(),
                    ensureCurrentDevice(
                            normalizedStatus.activatedDevices()
                    ),
                    "License verified. 1 device slot active."
            );
            case EXPIRED -> expiredStatus(
                    normalizedStatus.allowedSlots(),
                    ensureCurrentDevice(
                            normalizedStatus.activatedDevices()
                    ),
                    "License expired. Login blocked."
            );
            case GRACE_PERIOD -> graceStatus(
                    normalizedStatus.allowedSlots(),
                    ensureCurrentDevice(
                            normalizedStatus.activatedDevices()
                    ),
                    "License verification failed. Grace period active."
            );
            case DEVICE_LIMIT_EXCEEDED -> deviceLimitExceededStatus(
                    normalizedStatus
            );
            case AUTO -> {
                if (normalizedStatus.state() == LicenseState.EXPIRED
                        || normalizedStatus.state() == LicenseState.DEVICE_LIMIT_EXCEEDED) {
                    yield normalizedStatus;
                }

                if (!needsVerification(
                        normalizedStatus
                )) {
                    yield normalizedStatus;
                }

                yield activeStatus(
                        normalizedStatus.allowedSlots(),
                        ensureCurrentDevice(
                                normalizedStatus.activatedDevices()
                        ),
                        "License verified. 1 device slot active."
                );
            }
        };
    }

    private LicenseStatus normalize(
            LicenseStatus status
    ) {

        if (status == null) {
            return createFreshStatus();
        }

        if (status.state() == LicenseState.GRACE_PERIOD
                && status.graceUntil() != null
                && LocalDateTime.now().isAfter(
                status.graceUntil()
        )) {
            return expiredStatus(
                    status.allowedSlots(),
                    status.activatedDevices(),
                    "Grace period expired. Login blocked."
            );
        }

        if ((status.state() == LicenseState.ACTIVE
                || status.state() == LicenseState.GRACE_PERIOD)
                && activeDeviceCount(
                status.activatedDevices()
        ) > status.allowedSlots()) {
            return deviceLimitExceededStatus(
                    status
            );
        }

        return status;
    }

    private LicenseStatus createFreshStatus() {

        return activeStatus(
                DEFAULT_ALLOWED_SLOTS,
                List.of(
                        new LicenseDevice(
                                installationId,
                                deviceName,
                                LocalDateTime.now(),
                                LocalDateTime.now(),
                                true
                        )
                ),
                "License verified. 1 device slot active."
        );
    }

    private LicenseStatus activeStatus(
            int allowedSlots,
            List<LicenseDevice> devices,
            String message
    ) {

        LocalDateTime now =
                LocalDateTime.now();

        List<LicenseDevice> normalizedDevices =
                ensureCurrentDevice(
                        devices
                );

        return new LicenseStatus(
                LicenseState.ACTIVE,
                message,
                true,
                allowedSlots,
                normalizedDevices,
                installationId,
                now.plusDays(
                        VERIFICATION_INTERVAL_DAYS
                ),
                null,
                now
        );
    }

    private LicenseStatus graceStatus(
            int allowedSlots,
            List<LicenseDevice> devices,
            String message
    ) {

        LocalDateTime now =
                LocalDateTime.now();

        return new LicenseStatus(
                LicenseState.GRACE_PERIOD,
                message,
                true,
                allowedSlots,
                ensureCurrentDevice(
                        devices
                ),
                installationId,
                now.plusDays(
                        VERIFICATION_INTERVAL_DAYS
                ),
                now.plusDays(
                        DEFAULT_GRACE_DAYS
                ),
                now
        );
    }

    private LicenseStatus expiredStatus(
            int allowedSlots,
            List<LicenseDevice> devices,
            String message
    ) {

        LocalDateTime now =
                LocalDateTime.now();

        return new LicenseStatus(
                LicenseState.EXPIRED,
                message,
                false,
                allowedSlots,
                ensureCurrentDevice(
                        devices
                ),
                installationId,
                null,
                null,
                now
        );
    }

    private LicenseStatus deviceLimitExceededStatus(
            LicenseStatus baseStatus
    ) {

        List<LicenseDevice> devices =
                ensureOverflowDevices(
                        baseStatus
                );

        return new LicenseStatus(
                LicenseState.DEVICE_LIMIT_EXCEEDED,
                "Device limit exceeded. Owner action required.",
                false,
                baseStatus.allowedSlots(),
                devices,
                installationId,
                baseStatus.nextVerificationAt(),
                baseStatus.graceUntil(),
                LocalDateTime.now()
        );
    }

    private List<LicenseDevice> ensureOverflowDevices(
            LicenseStatus baseStatus
    ) {

        List<LicenseDevice> devices =
                new ArrayList<>(
                        baseStatus.activatedDevices()
                );

        LocalDateTime now =
                LocalDateTime.now();

        int syntheticIndex = 1;

        while (activeDeviceCount(
                devices
        ) <= baseStatus.allowedSlots()) {

            devices.add(
                    new LicenseDevice(
                            UUID.randomUUID().toString(),
                            "Additional device " + syntheticIndex,
                            now.minusDays(
                                    syntheticIndex
                            ),
                            now.minusDays(
                                    syntheticIndex - 1L
                            ),
                            true
                    )
            );

            syntheticIndex++;
        }

        if (!containsDevice(
                devices,
                installationId
        )) {
            devices.add(
                    new LicenseDevice(
                            installationId,
                            deviceName,
                            now,
                            now,
                            false
                    )
            );
        }

        return devices;
    }

    private boolean isCurrentDeviceActive(
            List<LicenseDevice> devices
    ) {

        for (LicenseDevice device : devices) {
            if (device.installationId().equals(
                    installationId
            )) {
                return device.active();
            }
        }

        return false;
    }

    private List<LicenseDevice> activateCurrentDevice(
            List<LicenseDevice> devices
    ) {

        List<LicenseDevice> activatedDevices =
                new ArrayList<>();

        boolean foundCurrentDevice =
                false;

        for (LicenseDevice device : devices) {

            if (device.installationId().equals(
                    installationId
            )) {
                foundCurrentDevice = true;
                activatedDevices.add(
                        new LicenseDevice(
                                installationId,
                                deviceName,
                                device.activatedAt() == null
                                        ? LocalDateTime.now()
                                        : device.activatedAt(),
                                LocalDateTime.now(),
                                true
                        )
                );
            } else {
                activatedDevices.add(
                        device
                );
            }
        }

        if (!foundCurrentDevice) {
            activatedDevices.add(
                    new LicenseDevice(
                            installationId,
                            deviceName,
                            LocalDateTime.now(),
                            LocalDateTime.now(),
                            true
                    )
            );
        }

        return activatedDevices;
    }

    private LicenseStatus statusFromDevices(
            int allowedSlots,
            LicenseState state,
            String message,
            List<LicenseDevice> devices,
            LocalDateTime nextVerificationAt,
            LocalDateTime graceUntil
    ) {

        boolean loginAllowed =
                state.isLoginAllowed();

        return new LicenseStatus(
                state,
                message,
                loginAllowed,
                allowedSlots,
                ensureCurrentDevice(
                        devices
                ),
                installationId,
                nextVerificationAt,
                graceUntil,
                LocalDateTime.now()
        );
    }

    private List<LicenseDevice> ensureCurrentDevice(
            List<LicenseDevice> devices
    ) {

        List<LicenseDevice> normalizedDevices =
                new ArrayList<>();

        boolean foundCurrentDevice =
                false;

        for (LicenseDevice device : devices) {

            if (device.installationId().equals(
                    installationId
            )) {
                foundCurrentDevice = true;
                normalizedDevices.add(
                        new LicenseDevice(
                                installationId,
                                deviceName,
                                device.activatedAt() == null
                                        ? LocalDateTime.now()
                                        : device.activatedAt(),
                                LocalDateTime.now(),
                                true
                        )
                );
            } else {
                normalizedDevices.add(
                        device
                );
            }
        }

        if (!foundCurrentDevice) {
            normalizedDevices.add(
                    new LicenseDevice(
                            installationId,
                            deviceName,
                            LocalDateTime.now(),
                            LocalDateTime.now(),
                            true
                    )
            );
        }

        return normalizedDevices;
    }

    private boolean needsVerification(
            LicenseStatus status
    ) {

        return status.nextVerificationAt() == null
                || !LocalDateTime.now().isBefore(
                status.nextVerificationAt()
        );
    }

    private boolean containsDevice(
            List<LicenseDevice> devices,
            String targetInstallationId
    ) {

        for (LicenseDevice device : devices) {
            if (device.installationId().equals(
                    targetInstallationId
            )) {
                return true;
            }
        }

        return false;
    }

    private int activeDeviceCount(
            List<LicenseDevice> devices
    ) {

        int count = 0;

        for (LicenseDevice device : devices) {
            if (device.active()) {
                count++;
            }
        }

        return count;
    }

    private void saveQuietly(
            LicenseStatus status
    ) {

        try {
            cacheStore.save(
                    status
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LicenseStatus loadCachedStatus() {

        try {
            Optional<LicenseStatus> cachedStatus =
                    cacheStore.load();

            if (cachedStatus.isPresent()) {
                return cachedStatus.get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        LicenseStatus fallbackStatus =
                createFreshStatus();

        saveQuietly(
                fallbackStatus
        );

        return fallbackStatus;
    }

    private String loadOrCreateInstallationId() {

        try {

            if (Files.exists(
                    INSTALLATION_ID_FILE
            )) {

                String existingId =
                        Files.readString(
                                INSTALLATION_ID_FILE,
                                StandardCharsets.UTF_8
                        ).trim();

                if (!existingId.isBlank()) {
                    return existingId;
                }
            }

            Files.createDirectories(
                    INSTALLATION_ID_FILE.getParent()
            );

            String generatedId =
                    UUID.randomUUID().toString();

            Files.writeString(
                    INSTALLATION_ID_FILE,
                    generatedId,
                    StandardCharsets.UTF_8
            );

            return generatedId;

        } catch (IOException e) {
            e.printStackTrace();
            return UUID.randomUUID().toString();
        }
    }

    private LicenseVerificationMode resolveSimulationMode() {

        String value =
                System.getProperty(
                        "storemanager.license.mock.mode",
                        LicenseVerificationMode.AUTO.name()
                );

        try {
            return LicenseVerificationMode.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (Exception e) {
            return LicenseVerificationMode.AUTO;
        }
    }

    private String resolveDeviceName() {

        try {
            String hostName =
                    InetAddress.getLocalHost().getHostName();

            if (hostName != null && !hostName.isBlank()) {
                return hostName;
            }
        } catch (UnknownHostException ignored) {
            // Fallback below.
        }

        return System.getProperty(
                "user.name",
                "device"
        ) + "-desktop";
    }
}
