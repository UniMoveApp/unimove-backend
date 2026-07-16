package com.unimove.unimove.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
        "senderUsername",
        "senderFullName",
        "content",
        "latitude",
        "longitude",
        "createdAt"
})
public class MessageResponse {
    private UUID id;
    private String senderUsername;
    private String senderFullName;
    private String content;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
}
