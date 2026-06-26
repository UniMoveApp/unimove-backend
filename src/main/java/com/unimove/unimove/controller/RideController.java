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
import java.util.UUID;

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

    @PutMapping("/{id}/start")
    public ResponseEntity<RideResponse> startRide(Principal principal, @PathVariable UUID id) {
        return ResponseEntity.ok(rideService.startRide(principal.getName(), id));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<RideResponse> completeRide(Principal principal, @PathVariable UUID id) {
        return ResponseEntity.ok(rideService.completeRide(principal.getName(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRide(Principal principal, @PathVariable UUID id) {
        rideService.deleteRide(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<RideResponse>> getMyRides(Principal principal, @RequestParam(required = false) String status) {
        return ResponseEntity.ok(rideService.getMyRides(principal.getName(), status));
    }

    @GetMapping("/archive")
    public ResponseEntity<List<RideResponse>> getArchive(Principal principal) {
        return ResponseEntity.ok(rideService.getArchive(principal.getName()));
    }
}
