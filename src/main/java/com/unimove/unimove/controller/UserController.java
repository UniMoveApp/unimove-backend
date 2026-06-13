package com.unimove.unimove.controller;

import com.unimove.unimove.dto.request.RoutePreferenceRequest;
import com.unimove.unimove.dto.request.UpdateIbanRequest;
import com.unimove.unimove.dto.request.UpdatePreferenceRequest;
import com.unimove.unimove.dto.response.RoutePreferenceResponse;
import com.unimove.unimove.dto.response.UserProfileResponse;
import com.unimove.unimove.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(Principal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getName()));
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<Void> updatePreferences(
            Principal principal,
            @RequestBody UpdatePreferenceRequest request) {
        userService.updatePreferences(principal.getName(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/iban")
    public ResponseEntity<Void> updateIban(
            Principal principal,
            @RequestBody UpdateIbanRequest request) {
        userService.updateIban(principal.getName(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/routes")
    public ResponseEntity<List<RoutePreferenceResponse>> getRoutes(Principal principal) {
        return ResponseEntity.ok(userService.getRoutes(principal.getName()));
    }

    @PostMapping("/me/routes")
    public ResponseEntity<RoutePreferenceResponse> addRoute(
            Principal principal,
            @RequestBody RoutePreferenceRequest request) {
        return ResponseEntity.ok(userService.addRoute(principal.getName(), request));
    }

    @DeleteMapping("/me/routes/{id}")
    public ResponseEntity<Void> deleteRoute(
            Principal principal,
            @PathVariable UUID id) {
        userService.deleteRoute(principal.getName(), id);
        return ResponseEntity.ok().build();
    }
}