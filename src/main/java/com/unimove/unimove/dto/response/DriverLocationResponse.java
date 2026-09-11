package com.unimove.unimove.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocationResponse {
    private UUID rideId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime updatedAt;
}
