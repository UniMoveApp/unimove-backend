package com.unimove.unimove.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMessageRequest {

    @NotBlank(message = "Il contenuto del messaggio non può essere vuoto")
    @Size(max = 1000, message = "Il messaggio non può superare i 1000 caratteri")
    private String content;

    private Double latitude;

    private Double longitude;
}
