package com.nekocafe.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * D-01 预约订单状态：created -> booked -> seated -> dining -> finished / cancelled / no_show。
 */
public enum ReservationStatus {
    CREATED("created"),
    BOOKED("booked"),
    SEATED("seated"),
    DINING("dining"),
    FINISHED("finished"),
    CANCELLED("cancelled"),
    NO_SHOW("no_show");

    private final String value;

    ReservationStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /** 占用桌位 / 阻塞时段的活跃状态。 */
    public boolean isActive() {
        return this == CREATED || this == BOOKED || this == SEATED || this == DINING;
    }

    public static Optional<ReservationStatus> from(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.value.equalsIgnoreCase(raw.trim()))
                .findFirst();
    }
}
