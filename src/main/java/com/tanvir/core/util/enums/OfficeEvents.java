package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum OfficeEvents {
	
	DAY_STARTED("DayStarted"),
	STAGED("Staged"),
	DAY_END_PROCESS_COMPLETED("DayEndProcessCompleted"),
	FORWARD_DAY_ROUTINE_COMPLETED("ForwardDayRoutineCompleted");
	
	private final String value;
	
	OfficeEvents(String value) {
		this.value = value;
	}
}
