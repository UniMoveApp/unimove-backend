package com.unimove.unimove.controller;

import com.unimove.unimove.dto.request.UpdateLocationRequest;
import com.unimove.unimove.dto.response.DriverLocationResponse;
import com.unimove.unimove.service.DriverLocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/rides/{rideId}/location")
public class DriverLocationController {

    private final DriverLocationService locationService;

    public DriverLocationController(DriverLocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    public ResponseEntity<DriverLocationResponse> updateLocation(Principal principal,
                                                                 @PathVariable UUID rideId,
                                                                 @Valid @RequestBody UpdateLocationRequest request) {
        return ResponseEntity.ok(locationService.updateLocation(principal.getName(), rideId, request));
    }

    @GetMapping
    public ResponseEntity<DriverLocationResponse> getLocation(Principal principal,
                                                              @PathVariable UUID rideId) {
        return ResponseEntity.ok(locationService.getLocation(principal.getName(), rideId));
    }
}
