package com.storemanager.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class TimeFormatUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private TimeFormatUtil() {
    }

    public static LocalDateTime truncateToSeconds(
            LocalDateTime value
    ) {

        if (value == null) {
            return null;
        }

        return value.withNano(0);
    }

    public static String formatDateTime(
            LocalDateTime value
    ) {

        if (value == null) {
            return "";
        }

        return DATE_TIME_FORMATTER.format(
                truncateToSeconds(value)
        );
    }

    public static LocalDateTime parseDateTime(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return LocalDateTime.parse(
                value.trim(),
                DATE_TIME_FORMATTER
        );
    }

    public static String formatTime(
            LocalTime value
    ) {

        if (value == null) {
            return "";
        }

        return TIME_FORMATTER.format(
                value.withNano(0)
        );
    }

    public static long hoursToSeconds(
            BigDecimal hours
    ) {

        if (hours == null) {
            return 0L;
        }

        return hours.multiply(BigDecimal.valueOf(3600L))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    public static String formatDurationHours(
            BigDecimal hours
    ) {

        if (hours == null) {
            return "";
        }

        return formatDurationSeconds(
                hoursToSeconds(hours)
        );
    }

    public static String formatDurationSeconds(
            long seconds
    ) {

        boolean negative = seconds < 0;
        long absoluteSeconds = Math.abs(seconds);

        long hours = absoluteSeconds / 3600L;
        long minutes = (absoluteSeconds % 3600L) / 60L;
        long remainingSeconds = absoluteSeconds % 60L;

        StringBuilder builder = new StringBuilder();

        if (negative) {
            builder.append('-');
        }

        if (hours > 0) {
            builder.append(hours).append('h').append(' ');
        }

        if (hours > 0 || minutes > 0) {
            builder.append(minutes).append('m').append(' ');
        }

        builder.append(remainingSeconds).append('s');

        return builder.toString().trim();
    }
}
