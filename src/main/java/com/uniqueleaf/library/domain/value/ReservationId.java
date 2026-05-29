package com.uniqueleaf.library.domain.value;

import java.util.Objects;
import java.util.UUID;

public final class ReservationId {
    private final String value;

    public ReservationId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ReservationId must not be blank");
        }
        this.value = value.trim();
    }

    public static ReservationId random() {
        return new ReservationId("RS-" + UUID.randomUUID());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ReservationId reservationId)) {
            return false;
        }
        return value.equals(reservationId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
