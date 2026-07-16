package com.unimove.unimove.service;

import com.unimove.unimove.dto.response.NotificationResponse;
import com.unimove.unimove.exception.ResourceNotFoundException;
import com.unimove.unimove.exception.UnauthorizedException;
import com.unimove.unimove.model.Notification;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.NotificationRepository;
import com.unimove.unimove.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Active SSE Emitters mapped by User ID
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Subscribe a user to the real-time notification stream.
     */
    public SseEmitter subscribe(UUID userId) {
        // Create emitter with 24 hours timeout
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L);

        try {
            // Send initial connection event to avoid connection timeout
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("Connected"));
        } catch (IOException e) {
            emitter.complete();
            return emitter;
        }

        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError((e) -> removeEmitter(userId, emitter));

        return emitter;
    }

    private void removeEmitter(UUID userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    /**
     * Create, persist, and send a notification to a specific user.
     */
    @Transactional
    public void sendNotification(User user, String type, String message, Ride ride) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .ride(ride)
                .build();

        notification = notificationRepository.save(notification);
        NotificationResponse response = mapToResponse(notification);

        // Push real-time event to all active emitters for this user
        List<SseEmitter> userEmitters = emitters.get(user.getId());
        if (userEmitters != null) {
            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .id(response.getId().toString())
                            .name("notification")
                            .data(response));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
            // Clean up disconnected emitters
            for (SseEmitter dead : deadEmitters) {
                removeEmitter(user.getId(), dead);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(String username, UUID notificationId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notifica non trovata"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Non sei autorizzato a modificare questa notifica");
        }

        notification.setRead(true);
        notification = notificationRepository.save(notification);

        return mapToResponse(notification);
    }

    @Transactional
    public void markAllAsRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        notificationRepository.markAllAsReadForUser(user);
    }

    @Transactional
    public void deleteNotification(String username, UUID notificationId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notifica non trovata"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Non sei autorizzato a eliminare questa notifica");
        }

        notificationRepository.delete(notification);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .rideId(notification.getRide() != null ? notification.getRide().getId() : null)
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
