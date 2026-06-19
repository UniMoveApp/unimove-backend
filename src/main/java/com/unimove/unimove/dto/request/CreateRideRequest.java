package com.unimove.unimove.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateRideRequest {

    @NotBlank(message = "Città di partenza obbligatoria")
    private String departureCity;

    @NotNull(message = "Orario di partenza obbligatorio")
    @Future(message = "L'orario di partenza deve essere nel futuro")
    private LocalDateTime departureTime;

    @NotBlank(message = "Città di arrivo obbligatoria")
    private String arrivalCity;

    private LocalDateTime arrivalTimeEst;

    @Size(max = 3, message = "Massimo 3 hotspot consentiti")
    private List<String> hotspots;

    private String vehicleModel;

    private String vehiclePlate;

    @NotNull(message = "Numero di posti obbligatorio")
    @Min(value = 1, message = "Deve esserci almeno un posto disponibile")
    @Max(value = 8, message = "Il numero di posti non può superare 8")
    private int totalSeats;

    private String travelPreferences;
}
