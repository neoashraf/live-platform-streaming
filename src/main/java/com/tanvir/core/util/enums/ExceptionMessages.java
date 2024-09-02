package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum ExceptionMessages {
	NO_LIVE_ROOM_FOUND_WITH_ID("No LiveRoom found with Id : "),
	USER_DOESNT_EXIST_IN_ROOM("User Doesn't Exist in Room"),
	INSUFFICIENT_BEANS("Insufficient Beans"),
	USER_NOT_FOUND("User Not Found"),
	;

	private final String value;
	
	ExceptionMessages(String value) {
		this.value = value;
	}
}
