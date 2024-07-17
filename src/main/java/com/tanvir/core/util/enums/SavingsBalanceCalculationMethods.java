package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum SavingsBalanceCalculationMethods {

    AVERAGE_DAILY_BALANCE("Average Daily Balance"),
    MINIMUM_DAILY_BALANCE("Minimum Daily Balance"),
    END_OF_DAY_BALANCE("End of Day Balance")
    ;

    private final String value;

    SavingsBalanceCalculationMethods(String value) {
        this.value = value;
    }
}
