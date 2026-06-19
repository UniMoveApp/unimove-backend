package com.unimove.unimove.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class RideResponse {
    private UUID id;
    private String driverUsername;
    private String driverFullName;
    private String departureCity;
    private LocalDateTime departureTime;
    private String arrivalCity;
    private LocalDateTime arrivalTimeEst;
    private List<String> hotspots;
    private String vehicleModel;
    private String vehiclePlate;
    private int totalSeats;
    private int availableSeats;
    private String travelPreferences;
    private String status;
}
