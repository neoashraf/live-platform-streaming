package com.tanvir.core.util.enums;

public enum Status {
    STATUS_ACTIVE("Active"),
    STATUS_INACTIVE("Inactive"),
    STATUS_APPROVED("Approved"),
    STATUS_WAITING("Waiting"),
    STATUS_PROCESSING("Processing"),
    STATUS_PENDING("Pending"),
    STATUS_FINISHED("Finished"),
    STATUS_STAGED("Staged"),
    STATUS_COMPLETED("Completed"),
    STATUS_INCOMPLETE("Incomplete"),
    STATUS_PAID("Paid"),
    STATUS_REJECTED("Rejected"),
    STATUS_CANCELLED("Canceled"),
    STATUS_RESCHEDULED("Rescheduled"),
    STATUS_FAILED("Failed"),
    STATUS_EXCEPTION("Exception");


    private final String value;

    Status(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
