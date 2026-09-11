package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.CreateMessageRequest;
import com.unimove.unimove.dto.response.MessageResponse;
import com.unimove.unimove.exception.InvalidRequestException;
import com.unimove.unimove.exception.ResourceNotFoundException;
import com.unimove.unimove.exception.UnauthorizedException;
import com.unimove.unimove.model.Message;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.BookingRepository;
import com.unimove.unimove.repository.MessageRepository;
import com.unimove.unimove.repository.RideRepository;
import com.unimove.unimove.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private static final String UTENTE_NON_TROVATO = "Utente non trovato";
    private static final String CORSA_NON_TROVATA = "Corsa non trovata";
    private static final String ACCESSO_NEGATO = "Non hai accesso alla chat di questa corsa";
    private static final String CORSA_TERMINATA = "Non puoi inviare messaggi in una corsa già terminata";

    private final MessageRepository messageRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final MessageMapper messageMapper;

    public MessageService(MessageRepository messageRepository,
                          RideRepository rideRepository,
                          UserRepository userRepository,
                          BookingRepository bookingRepository,
                          MessageMapper messageMapper) {
        this.messageRepository = messageRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.messageMapper = messageMapper;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesForRide(String username, UUID rideId) {
        User user = getUser(username);
        Ride ride = getRide(rideId);

        // Verifica che l'utente sia il driver oppure un passeggero confermato
        verifyUserAccess(user, ride);

        return messageRepository.findByRideOrderByCreatedAtAsc(ride)
                .stream()
                .map(messageMapper::toResponse)
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(String username, UUID rideId, CreateMessageRequest request) {
        User sender = getUser(username);
        Ride ride = getRide(rideId);

        // Verifica che l'utente sia il driver oppure un passeggero confermato
        verifyUserAccess(sender, ride);

        // Verifica che la corsa sia ancora attiva
        if ("COMPLETED".equals(ride.getStatus())) {
            throw new InvalidRequestException(CORSA_TERMINATA);
        }

        Message message = Message.builder()
                .ride(ride)
                .sender(sender)
                .content(request.getContent())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        messageRepository.save(message);

        return messageMapper.toResponse(message);
    }

    @Transactional
    public void deleteMessagesForRide(Ride ride) {
        messageRepository.deleteByRide(ride);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(UTENTE_NON_TROVATO));
    }

    private Ride getRide(UUID rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException(CORSA_NON_TROVATA));
    }

    private void verifyUserAccess(User user, Ride ride) {
        boolean isDriver = ride.getDriver().getId().equals(user.getId());
        boolean isConfirmedPassenger = bookingRepository.existsByRideAndPassengerAndStatus(ride, user, "CONFIRMED");

        if (!isDriver && !isConfirmedPassenger) {
            throw new UnauthorizedException(ACCESSO_NEGATO);
        }
    }
}
