package com.unimove.unimove.service;

import com.unimove.unimove.dto.response.MessageResponse;
import com.unimove.unimove.model.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .senderUsername(message.getSender().getUsername())
                .senderFullName(message.getSender().getFullName())
                .content(message.getContent())
                .latitude(message.getLatitude())
                .longitude(message.getLongitude())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
