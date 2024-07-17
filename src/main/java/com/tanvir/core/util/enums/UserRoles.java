package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum UserRoles {

    MFI_BRANCH_MANAGER("MFI-BRANCH-MANAGER"),
    MFI_FIELD_OFFICER("MFI-FIELD-OFFICER");

    private final String value;

    UserRoles (String value){
        this.value = value;
    }
}
