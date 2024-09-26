package com.tanvir.features.agora.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgoraTokenResponseDto {
    private String message;
    private List<AgoraToken> data;
    private int count;
    private boolean error;
}
