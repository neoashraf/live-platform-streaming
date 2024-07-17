package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum CollectionType {
	REGULAR("Regular"),
	SPECIAL("Special"),
	;
	private final String value;
	CollectionType(String value){
		this.value = value;
	}
	

}
