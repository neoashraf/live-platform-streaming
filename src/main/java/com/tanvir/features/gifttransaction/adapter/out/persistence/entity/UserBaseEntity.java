package com.tanvir.features.gifttransaction.adapter.out.persistence.entity;

import lombok.Data;

@Data
public class UserBaseEntity {
    private double beans;
    private double beansSent;

    private String userType;
    private String id;
    private String maxId;
    private String displayName;
    private String profileImageId;
    private String profileImageUrl;

}
