package com.unimove.unimove.service;

import com.unimove.unimove.dto.response.RideResponse;
import com.unimove.unimove.model.Ride;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RideMapper {

    public RideResponse toResponse(Ride ride) {
        List<String> hotspots = new ArrayList<>();
        if(ride.getHotspot1() != null)
            hotspots.add(ride.getHotspot1());
        if(ride.getHotspot2() != null)
            hotspots.add(ride.getHotspot2());
        if(ride.getHotspot3() != null)
            hotspots.add(ride.getHotspot3());

        return RideResponse.builder()
                .id(ride.getId())
                .driverUsername(ride.getDriver().getUsername())
                .driverFullName(ride.getDriver().getFullName())
                .departureCity(ride.getDepartureCity())
                .departureTime(ride.getDepartureTime())
                .arrivalCity(ride.getArrivalCity())
                .arrivalTimeEst(ride.getArrivalTimeEst())
                .hotspots(hotspots)
                .vehicleModel(ride.getVehicleModel())
                .vehiclePlate(ride.getVehiclePlate())
                .totalSeats(ride.getTotalSeats())
                .availableSeats(ride.getAvailableSeats())
                .travelPreferences(ride.getTravelPreferences())
                .status(ride.getStatus())
                .build();
    }
}
