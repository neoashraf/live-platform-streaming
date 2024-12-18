package com.tanvir.features.liveroomsummary.domain.dto;

import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDetail {
    private String userId;
    private List<LiveRoomEntity> sessions;
}
