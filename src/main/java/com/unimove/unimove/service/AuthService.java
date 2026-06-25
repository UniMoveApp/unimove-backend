package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.LoginRequest;
import com.unimove.unimove.dto.response.CinecaLoginResponse;
import com.unimove.unimove.dto.response.LoginResponse;
import com.unimove.unimove.model.Role;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.UserRepository;
import com.unimove.unimove.security.JwtUtil;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RestClient restClient;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, RestClient restClient) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.restClient = restClient;
    }

    public LoginResponse login(LoginRequest request) {

        String credentials = request.getUsername() + ":" + request.getPassword();
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        CinecaLoginResponse cinecaResponse;
        try{
            cinecaResponse = restClient.get()
                    .uri("https://unimol.esse3.cineca.it/e3rest/api/login?optionalFields=jwt")
                    .header("Authorization", basicAuth)
                    .header("Accept", "application/json")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) ->{
                        throw new RuntimeException("Credenziali non valide");
                    })
                    .body(CinecaLoginResponse.class);
        } catch(Exception e) {
            throw new RuntimeException("Credenziali non valide");
        }

        if(cinecaResponse == null || cinecaResponse.getUser() == null) {
            throw new RuntimeException("Risposta CINECA non valida");
        }

        CinecaLoginResponse.UserInfo userInfo = cinecaResponse.getUser();

        Role role = mapRole(userInfo.getGrpDes());

        String email = buildEmail(request.getUsername(), role);

        String fullName = userInfo.getFirstName() + " " + userInfo.getLastName();

        Optional<User> existing = userRepository.findByUsername(request.getUsername());
        User user;

        if(existing.isEmpty()) {
            user = User.builder()
                    .username(request.getUsername())
                    .email(email)
                    .fullName(fullName)
                    .role(role)
                    .persId(userInfo.getPersId())
                    .avatarUrl(fetchAvatarAsBase64(request.getUsername(), request.getPassword(), userInfo.getPersId()))
                    .build();
        } else {
            user = existing.get();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPersId(userInfo.getPersId());
            user.setAvatarUrl(fetchAvatarAsBase64(request.getUsername(), request.getPassword(), userInfo.getPersId()));
        }

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        return new LoginResponse(token, user.getUsername(), user.getRole().name(), user.getFullName());
    }

    public LoginResponse register(LoginRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username già esistente");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getUsername() + "@studenti.unimol.it")
                .fullName(request.getFullName() != null ? request.getFullName() : request.getUsername())
                .role(Role.STUDENT)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, user.getUsername(), user.getRole().name(), user.getFullName());
    }

    private Role mapRole(String grpDes) {

        if(grpDes == null)
            return Role.STUDENT;
        return switch(grpDes.toUpperCase()) {
            case "DOCENTI" -> Role.PROFESSOR;
            case "PTA", "DIPENDENTI" -> Role.STAFF;
            default -> Role.STUDENT;
        };
    }

    private String buildEmail(String username, Role role) {
        return switch(role) {
            case STUDENT -> username + "@studenti.unimol.it";
            case PROFESSOR, STAFF -> username + "@docenti.unimol.it";
        };
    }

    private String fetchAvatarAsBase64(String username, String password, Long persId) {
        if (persId == null) return null;

        String credentials = username + ":" + password;
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        try {
            byte[] imageBytes = restClient.get()
                    .uri("https://unimol.esse3.cineca.it/e3rest/api/anagrafica-service-v2/persone/" + persId + "/foto")
                    .header("Authorization", basicAuth)
                    .header("Accept", "application/octet-stream")
                    .retrieve()
                    .body(byte[].class);

            if (imageBytes == null || imageBytes.length == 0) return null;

            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            return null;
        }
    }
}