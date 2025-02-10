package com.tanvir.features.liveroom.domain;

import lombok.Getter;

import java.util.List;

@Getter
public enum OfficialIdEnum {

    OFFICIAL_IDS(List.of(
            "10000001",
            "10000002",
            "10000003",
            "10000004",
            "10000008",
            "10000015",
            "10000023",
            "10000467",
            "10000999",
            "10001000",
            "10000465",
            "10000475")),

    SUPER_USER_IDS(List.of(
            "10000001",
            "10000475"));

    private final List<String> value;

    OfficialIdEnum(List<String> value) {
        this.value = value;
    }
}
