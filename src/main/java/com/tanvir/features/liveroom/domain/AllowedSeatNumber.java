package com.tanvir.features.liveroom.domain;

import java.util.Arrays;

public enum AllowedSeatNumber {
    SEAT_8(8),
    SEAT_10(10),
    SEAT_12(12),
    SEAT_15(15);

    private final int seatNumber;

    AllowedSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public static boolean isValid(int seatNumber) {
        return Arrays.stream(AllowedSeatNumber.values())
                .anyMatch(seat -> seat.getSeatNumber() == seatNumber);
    }
}
