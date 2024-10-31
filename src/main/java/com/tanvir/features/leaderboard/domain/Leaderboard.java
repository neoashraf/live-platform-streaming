package com.tanvir.features.leaderboard.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Leaderboard {
   private Double totalGiftAmount;
   private String totalGiftAmountValue;
   private List<GiftSummaryUser> topGiftSenders;
   private List<GiftSummaryUser> topHosts;
   private List<GiftSummaryUser> topAgencies;
}
