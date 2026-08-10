package com.schemebridge.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private DateUtils() {
    }

    public static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String DATE_ONLY_FORMAT = "yyyy-MM-dd";

    public static String formatInstantIso(Instant instant) {
        if (instant == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ISO_FORMAT).withZone(ZoneId.of("UTC"));
        return formatter.format(instant);
    }

    public static String formatDateOnly(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_ONLY_FORMAT);
        return dateTime.format(formatter);
    }
}
