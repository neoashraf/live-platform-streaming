package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum SavingsProductType {
	PRODUCT_TYPE_GS("GS"),
	PRODUCT_TYPE_VS("VS");
	
	private final String value;
	
	SavingsProductType(String value){
		this.value = value;
	}
}
