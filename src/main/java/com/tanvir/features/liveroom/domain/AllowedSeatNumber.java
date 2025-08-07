package com.tanvir.features.liveroom.domain;

import java.util.Arrays;

public enum AllowedSeatNumber {
    SEAT_5(5),
    SEAT_6(6),
    SEAT_9(9),
    SEAT_12(12);

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
