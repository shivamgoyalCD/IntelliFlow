package com.shivam.intelliflow.common.util;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class TimestampUtils {
    private static final Clock UTC_CLOCK = Clock.systemUTC();

    private TimestampUtils() {
    }

    public static Instant nowUtc() {
        return Instant.now(UTC_CLOCK);
    }

    public static Instant now(Clock clock) {
        return Instant.now(Objects.requireNonNull(clock, "clock must not be null"));
    }

    public static String nowUtcIso() {
        return DateTimeFormatter.ISO_INSTANT.format(nowUtc());
    }

    public static long nowUtcEpochMillis() {
        return nowUtc().toEpochMilli();
    }
}
