package com.tanvir.features.liveroomsummary.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HostEarningRequestDto {
    private String keycloakId;
    private int month;
    private int year;
}
