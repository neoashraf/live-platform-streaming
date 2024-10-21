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
public class AgoraTokenGenerationResponseDto {
    private String message;
    private AgoraToken data;
    private int count;
    private boolean error;
}
