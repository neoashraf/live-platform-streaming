package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum ExceptionMessages {
	
	// DISBURSEMENT
	LOAN_DISBURSEMENT_FAILED("Loan Disbursement Failed"),
	LOAN_REPAYMENT_SCHEDULE_ALREADY_GENERATED("Loan Repayment Schedule Is Already Generated!"),
	LOAN_ACCOUNT_STATUS_NOT_APPROVED("Loan Account Status Is Not 'Approved'!"),
	
	// COLLECTION STAGING DATA
	NO_COLLECTION_STAGING_DATA_FOUND_FOR_OFFICE("No Collection Staging Data Found for Office."),
	NO_STAGING_DATA_FOUND("No Staging Data Found."),
	NO_STAGING_DATA_FOUND_FOR_SAMITY("No Staging Data Found For Samity : "),
	NO_STAGING_DATA_FOUND_WITH_ACCOUNT_ID("No Staging Data Found With Account ID : "),
	NO_STAGING_DATA_FOUND_WITH_FIELD_OFFICER_ID("No Staging Data Found With Field Officer ID : "),
	
	// INTERNAL SERVER ERROR
	SOMETHING_WENT_WRONG("Something Went Wrong. Please Try Again Later."),
	
	// AUTHORIZE COLLECTION
	NO_COLLECTION_DATA_FOUND("No Collection Data Found"),
	
	// PAYMENT COLLECTION
	COLLECTION_DATA_MISMATCH_FOR_SAMITY("Collection Data Mismatch for Samity."),
	COLLECTION_DATA_MISMATCH_FOR_STAGING_DATA_ID_AND_ACCOUNT_ID("Collection Data Mismatch for Staging Data ID And Account ID."),
	COLLECTION_DATA_MISMATCH_FOR_COLLECTION_TYPE("Collection Data Mismatch for Collection Type."),
	
	// PASSBOOK
	
	VALIDATION_FAILED("Validation Failed!"),
	WITHDRAW_NOT_ALLOWED_NO_DEPOSIT_RECORD("No Deposit Record in Savings Account. Withdraw not allowed."),
	SAVINGS_ACCOUNT_STATUS_IS_NOT_ACTIVE("Savings Account Status Is Not 'Active'."),
	SAVINGS_ACCOUNT_STATUS_INVALID("Savings Account Status 'Invalid'!"),
	PASSBOOK_ENTRY_EXISTS_YET_SAVINGS_ACCOUNT_STATUS_NOT_ACTIVE("Passbook Entry Exists But Savings Account Status 'Inactive'"),
	
	// WITHDRAW
	NO_WITHDRAW_DATA_FOUND("No Withdraw Data Found!"),
	INSUFFICIENT_BALANCE_FOR_WITHDRAW_IN_SAVINGS_ACCOUNT("Insufficient Balance for Withdraw In Savings Account : "),
	
	NO_SAMITY_FOUND_FOR_OFFICER_LIST("No Samity Found For Officer List"),
	NO_WITHDRAW_STAGING_DATA_FOUND_FOR_OFFICE("No Withdraw Staging Data Found For Office"),

	SAMITY_CANNOT_BE_CANCELED("Regular Samity cannot be Canceled."),
	SAMITY_CANCELED("Samity Canceled Successfully : "),

	//    TRANSACTION
	FAILED_TO_SAVE_TRANSACTION("Failed to save Transaction"),

	// ACCRUED INTEREST

	INTEREST_ALREADY_ACCRUED("Interest already accrued for this month."),
	INTEREST_CANNOT_BE_CALCULATED("Interest Cannot be calculated for month before A/C opening date."),

	// FDR

	ACCOUNT_ALREADY_ACTIVATED("Account Already Activated"),
	MINIMUM_AMOUNT_REQUIREMENT_NOT_MET("Minimum Amount Requirement Not Met."),
	AMOUNT_EXCEEDED_MAXIMUM_AMOUNT("FDR Amount Exceeded Maximum Amount."),
	FDR_AMOUNT_MISMATCH("FDR Amount Mismatch!"),
	ACCOUNT_NOT_ELIGIBLE_FOR_ACTIVATION("Account Not Eligible For Activation!"),
	SAVINGS_ACCOUNT_NOT_FOUND("Savings Account Not Found."),
	SCHEDULE_ALREADY_EXISTS_FOR("Schedule Already exists for : ")
	;

	private final String value;
	
	ExceptionMessages(String value) {
		this.value = value;
	}
}
