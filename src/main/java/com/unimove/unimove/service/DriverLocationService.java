package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.UpdateLocationRequest;
import com.unimove.unimove.dto.response.DriverLocationResponse;
import com.unimove.unimove.exception.InvalidRequestException;
import com.unimove.unimove.exception.ResourceNotFoundException;
import com.unimove.unimove.exception.UnauthorizedException;
import com.unimove.unimove.model.DriverLocation;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.BookingRepository;
import com.unimove.unimove.repository.DriverLocationRepository;
import com.unimove.unimove.repository.RideRepository;
import com.unimove.unimove.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DriverLocationService {

    private final DriverLocationRepository locationRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public DriverLocationService(DriverLocationRepository locationRepository,
                                 RideRepository rideRepository,
                                 UserRepository userRepository,
                                 BookingRepository bookingRepository) {
        this.locationRepository = locationRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public DriverLocationResponse updateLocation(String username, UUID rideId, UpdateLocationRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Corsa non trovata"));

        if (!ride.getDriver().getId().equals(user.getId())) {
            throw new UnauthorizedException("Non sei il guidatore di questa corsa");
        }

        if (!"IN_PROGRESS".equals(ride.getStatus())) {
            throw new InvalidRequestException("La corsa non è in corso");
        }

        DriverLocation location = locationRepository.findById(rideId)
                .orElseGet(() -> DriverLocation.builder().ride(ride).build());

        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setUpdatedAt(LocalDateTime.now());

        locationRepository.save(location);

        return DriverLocationResponse.builder()
                .rideId(rideId)
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .updatedAt(location.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public DriverLocationResponse getLocation(String username, UUID rideId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Corsa non trovata"));

        // Verify if requester is driver or accepted passenger
        boolean isDriver = ride.getDriver().getId().equals(user.getId());
        boolean isPassenger = bookingRepository.existsByRideAndPassengerAndStatus(ride, user, "CONFIRMED");

        if (!isDriver && !isPassenger) {
            throw new UnauthorizedException("Non sei autorizzato a vedere la posizione per questa corsa");
        }

        DriverLocation location = locationRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Posizione non disponibile per questa corsa"));

        return DriverLocationResponse.builder()
                .rideId(rideId)
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .updatedAt(location.getUpdatedAt())
                .build();
    }
}
