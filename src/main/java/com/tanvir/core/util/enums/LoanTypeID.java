package com.tanvir.core.util.enums;

public enum LoanTypeID {

    LOAN_TYPE_M("M");

    private final String value;

    LoanTypeID(String value){
        this.value = value;
    }

    public String getValue(){
        return this.value;
    }
}
