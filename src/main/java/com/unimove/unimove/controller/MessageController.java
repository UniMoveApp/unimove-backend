package com.unimove.unimove.controller;

import com.unimove.unimove.dto.request.CreateMessageRequest;
import com.unimove.unimove.dto.response.MessageResponse;
import com.unimove.unimove.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rides/{rideId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(Principal principal, @PathVariable UUID rideId) {
        return ResponseEntity.ok(messageService.getMessagesForRide(principal.getName(), rideId));
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            Principal principal,
            @PathVariable UUID rideId,
            @Valid @RequestBody CreateMessageRequest request) {
        return ResponseEntity.status(201).body(messageService.sendMessage(principal.getName(), rideId, request));
    }
}
