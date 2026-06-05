package com.storemanager.domain.onlineorder.model;

import java.util.Map;
import java.util.Set;

public final class OnlineOrderStatus {

    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String PREPARING = "PREPARING";
    public static final String READY = "READY";
    public static final String DELIVERING = "DELIVERING";
    public static final String DELIVERED = "DELIVERED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String REFUNDED = "REFUNDED";
    public static final String REJECTED = "REJECTED";

    private static final Set<String> KNOWN_STATUSES = Set.of(
            PENDING,
            CONFIRMED,
            PREPARING,
            READY,
            DELIVERING,
            DELIVERED,
            COMPLETED,
            CANCELLED,
            REFUNDED,
            REJECTED
    );

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, Set.of(CONFIRMED, CANCELLED, REJECTED),
            CONFIRMED, Set.of(PREPARING, CANCELLED, REJECTED),
            PREPARING, Set.of(READY, CANCELLED, REJECTED),
            READY, Set.of(DELIVERING, CANCELLED, REJECTED),
            DELIVERING, Set.of(DELIVERED, CANCELLED),
            DELIVERED, Set.of(COMPLETED, REFUNDED),
            COMPLETED, Set.of(REFUNDED),
            CANCELLED, Set.of(),
            REFUNDED, Set.of(),
            REJECTED, Set.of()
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
