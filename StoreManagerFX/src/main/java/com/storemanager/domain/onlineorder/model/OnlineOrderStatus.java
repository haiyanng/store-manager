package com.storemanager.domain.onlineorder.model;

import java.util.Map;
import java.util.Set;

public final class OnlineOrderStatus {

    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String PREPARING = "PREPARING";
    public static final String DELIVERING = "DELIVERING";
    public static final String DELIVERED = "DELIVERED";
    public static final String CANCELLED = "CANCELLED";

    private static final Set<String> KNOWN_STATUSES = Set.of(
            PENDING,
            CONFIRMED,
            PREPARING,
            DELIVERING,
            DELIVERED,
            CANCELLED
    );

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(PREPARING, CANCELLED),
            PREPARING, Set.of(DELIVERING, CANCELLED),
            DELIVERING, Set.of(DELIVERED, CANCELLED),
            DELIVERED, Set.of(),
            CANCELLED, Set.of()
    );

    private OnlineOrderStatus() {
    }

    public static boolean isKnown(String status) {
        return status != null && KNOWN_STATUSES.contains(status);
    }

    public static boolean canTransition(String currentStatus, String nextStatus) {
        return isKnown(currentStatus)
                && isKnown(nextStatus)
                && !currentStatus.equals(nextStatus)
                && ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus);
    }

    public static Set<String> knownStatuses() {
        return KNOWN_STATUSES;
    }
}
