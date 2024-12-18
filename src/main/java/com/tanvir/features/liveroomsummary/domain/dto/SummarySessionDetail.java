package com.tanvir.features.liveroomsummary.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SummarySessionDetail {
    private String userId;
    private List<Map<String, List<FilterSummary>>> completeSessionDetails;
}
