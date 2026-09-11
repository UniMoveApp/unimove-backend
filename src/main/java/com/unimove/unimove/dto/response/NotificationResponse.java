package com.unimove.unimove.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({
        "id",
        "userId",
        "type",
        "message",
        "isRead",
        "rideId",
        "createdAt"
})
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private String type;
    private String message;

    @JsonProperty("isRead")
    private boolean isRead;

    private UUID rideId;
    private LocalDateTime createdAt;
}
