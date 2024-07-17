package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum QueryParams {
    OFFICE_ID("officeId"),
    SAMITY_ID("samityId"),
    FIELD_OFFICER_ID("fieldOfficerId"),
    MFI_ID("mfiId"),
    LOGIN_ID("loginId"),
    ACCOUNT_ID("accountId"),
    EMPLOYEE_ID("employeeId"),
    USER_ROLE("userRole"),
    ACCOUNT_NO("accountNo"),
    INSTITUTE_OID("instituteOid"),
    DISBURSEMENT_DATE("disbursementDate"),
    INSTALLMENT_NO("installmentNo"),
    AMOUNT("amount"),
    /*TRANSACTION_CODE_LOAN_REPAY("LOAN_REPAY"),
    TRANSACTION_CODE_SAVINGS_DEPOSIT("deposit"),
    TRANSACTION_CODE_SAVINGS_WITHDRAW("withdraw"),*/
    TRANSACTION_CODE("transactionCode"),
    TRANSACTION_DATE("transactionDate"),
    LOAN_ACCOUNT_ID("loanAccountId"),
    SAVINGS_ACCOUNT_ID("savingsAccountId"),
    TRANSACTION_ID("transactionId"),
    MEMBER_ID("memberId"),
    MANAGEMENT_PROCESS_ID("managementProcessId"),

    ACCRUED_INTEREST_AMOUNT("accruedInterestAmount"),
    CLOSING_TYPE("closingType"),

    FROM_DATE("fromDate"),
    TO_DATE("toDate"),
    SEARCH_TEXT("searchText"),
    LIMIT("limit"),
    OFFSET("offset"),

    INTEREST_POSTING_DATE("interestPostingDate"),
    FDR_ACTIVATION_DATE("fdrActivationDate"),

    INTEREST_RATE("interestRate"),
    INTEREST_RATE_FREQUENCY("interestRateFrequency"),
    INTEREST_RATE_PRECISION("interestRatePrecision"),
    ACCRUED_INTEREST_PRECISION("accruedInterestPrecision"),
    ROUNDING_MODE("roundingMode"),
    DAYS_IN_YEAR("daysInYear"),
    INTEREST_CALCULATION_DATE("interestCalculationDate"),
    INTEREST_CALCULATION_MONTH("interestCalculationMonth"),
    INTEREST_CALCULATION_YEAR("interestCalculationYear"),
    BALANCE_CALCULATION_METHOD("balanceCalculationMethod"),
    REMARKS("remarks");

    private final String value;

    QueryParams(String value) {
        this.value = value;
    }
}
