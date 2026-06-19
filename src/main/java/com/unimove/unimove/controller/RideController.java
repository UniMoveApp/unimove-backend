package com.unimove.unimove.controller;

import com.unimove.unimove.dto.request.CreateRideRequest;
import com.unimove.unimove.dto.response.RideResponse;
import com.unimove.unimove.service.RideService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    public ResponseEntity<RideResponse> createRide(Principal principal, @Valid @RequestBody CreateRideRequest request) {
        return ResponseEntity.ok(rideService.createRide(principal.getName(), request));
    }

    @GetMapping("/search")
    public ResponseEntity<List<RideResponse>> searchRides(
            @RequestParam(required = false) String driverUsername,
            @RequestParam(required = false) String departureCity,
            @RequestParam(required = false) String arrivalCity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(rideService.searchRide(driverUsername, departureCity, arrivalCity, date));
    }
}
