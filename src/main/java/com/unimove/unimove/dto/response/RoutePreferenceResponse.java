package com.unimove.unimove.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePreferenceResponse {
    private UUID id;
    private String cityFrom;
    private String cityTo;
}
