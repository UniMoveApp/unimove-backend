package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.CreateRideRequest;
import com.unimove.unimove.dto.response.RideResponse;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.RideRepository;
import com.unimove.unimove.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final RideMapper rideMapper;

    public RideService(RideRepository rideRepository, UserRepository userRepository, RideMapper rideMapper) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.rideMapper = rideMapper;
    }

    public RideResponse createRide(String username, CreateRideRequest request) {

        User driver = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        List<String> hotspots = request.getHotspots();

        Ride ride = Ride.builder()
                .driver(driver)
                .departureCity(request.getDepartureCity())
                .departureTime(request.getDepartureTime())
                .arrivalCity(request.getArrivalCity())
                .arrivalTimeEst(request.getArrivalTimeEst())
                .hotspot1(getHotspot(hotspots, 0))
                .hotspot2(getHotspot(hotspots, 1))
                .hotspot3(getHotspot(hotspots, 2))
                .vehicleModel(request.getVehicleModel())
                .vehiclePlate(request.getVehiclePlate())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .travelPreferences(request.getTravelPreferences())
                .build();

        rideRepository.save(ride);

        return rideMapper.toResponse(ride);
    }

    @Transactional(readOnly = true)
    public List<RideResponse> searchRide(String driverUsername, String departureCity, String arrivalCity, LocalDate date) {
        return rideRepository.search(driverUsername, departureCity, arrivalCity, date)
                .stream()
                .map(rideMapper::toResponse)
                .toList();
    }

    private String getHotspot(List<String> hotspots, int index) {
        if(hotspots == null || hotspots.size() <= index) {
            return null;
        }
        return hotspots.get(index);
    }
}
