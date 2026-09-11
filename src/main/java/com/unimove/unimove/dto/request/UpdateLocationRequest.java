package com.unimove.unimove.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLocationRequest {

    @NotNull(message = "La latitudine è obbligatoria")
    private Double latitude;

    @NotNull(message = "La longitudine è obbligatoria")
    private Double longitude;
}
