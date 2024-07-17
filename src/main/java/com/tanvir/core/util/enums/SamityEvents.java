package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum SamityEvents {
//	Samity Cancellation
	CANCELED("Canceled"),
//	Samity Collection
	COLLECTED("Collected"),
	COLLECTION_AUTHORIZED("CollectionAuthorized"),
	COLLECTION_TRANSACTION_COMPLETED("CollectionTransactionCompleted"),
	COLLECTION_PASSBOOK_COMPLETED("CollectionPassbookCompleted"),
//	Samity Withdraw
	WITHDRAWN("Withdrawn"),
	WITHDRAW_AUTHORIZED("WithdrawAuthorized"),
	WITHDRAW_TRANSACTION_COMPLETED("WithdrawTransactionCompleted"),
	WITHDRAW_PASSBOOK_COMPLETED("WithdrawPassbookCompleted");
	
	private final String value;
	
	SamityEvents(String value) {
		this.value = value;
	}
}
