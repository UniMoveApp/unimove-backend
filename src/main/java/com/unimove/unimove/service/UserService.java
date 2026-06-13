package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.RoutePreferenceRequest;
import com.unimove.unimove.dto.request.UpdateIbanRequest;
import com.unimove.unimove.dto.request.UpdatePreferenceRequest;
import com.unimove.unimove.dto.response.RoutePreferenceResponse;
import com.unimove.unimove.dto.response.UserProfileResponse;
import com.unimove.unimove.model.RoutePreference;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.RoutePreferenceRepository;
import com.unimove.unimove.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoutePreferenceRepository routePreferenceRepository;

    public UserService(UserRepository userRepository, RoutePreferenceRepository routePreferenceRepository) {
        this.userRepository = userRepository;
        this.routePreferenceRepository = routePreferenceRepository;
    }

    public UserProfileResponse getProfile(String username) {
        User user = getUser(username);
        return UserProfileResponse.builder()
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .travelPreferences(user.getTravelPreferences())
                .iban(user.getIban())
                .ibanHolder(user.getIbanHolder())
                .build();
    }

    public void updatePreferences(String username, UpdatePreferenceRequest request) {
        User user = getUser(username);
        user.setTravelPreferences(request.getTravelPreferences());
        userRepository.save(user);
    }

    public void updateIban(String username, UpdateIbanRequest request) {
        User user = getUser(username);
        user.setIban(request.getIban());
        user.setIbanHolder(request.getIbanHolder());
        userRepository.save(user);
    }

    public List<RoutePreferenceResponse> getRoutes(String username) {
        User user = getUser(username);
        return routePreferenceRepository.findByUser(user).stream()
                .map(r -> RoutePreferenceResponse.builder()
                        .id(r.getId())
                        .cityFrom(r.getCityFrom())
                        .cityTo(r.getCityTo())
                        .build())
                .toList();
    }

    public RoutePreferenceResponse addRoute(String username, RoutePreferenceRequest request) {
        User user = getUser(username);

        if(routePreferenceRepository.countByUser(user) >= 3) {
            throw new RuntimeException("Massimo 3 tratte preferite consentite");
        }

        RoutePreference route = RoutePreference.builder()
                .user(user)
                .cityFrom(request.getCityFrom())
                .cityTo(request.getCityTo())
                .build();
        routePreferenceRepository.save(route);

        return RoutePreferenceResponse.builder()
                .id(route.getId())
                .cityFrom(route.getCityFrom())
                .cityTo(route.getCityTo())
                .build();
    }

    public void deleteRoute(String username, UUID routeId){
        User user = getUser(username);
        RoutePreference route = routePreferenceRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Tratta preferita non trovata"));

        if(!route.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Non sei autorizzato a eliminare questa tratta preferita");
        }

        routePreferenceRepository.delete(route);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
    }
}
