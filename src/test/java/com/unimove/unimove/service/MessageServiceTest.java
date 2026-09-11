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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MessageMapper messageMapper;

    private MessageService messageService;

    private User driver;
    private User passenger;
    private User externalUser;
    private Ride ride;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(
                messageRepository,
                rideRepository,
                userRepository,
                bookingRepository,
                messageMapper
        );

        driver = User.builder()
                .id(UUID.randomUUID())
                .username("driver.unimol")
                .fullName("Mario Rossi")
                .build();

        passenger = User.builder()
                .id(UUID.randomUUID())
                .username("passenger.unimol")
                .fullName("Luigi Bianchi")
                .build();

        externalUser = User.builder()
                .id(UUID.randomUUID())
                .username("external.unimol")
                .fullName("Carlo Neri")
                .build();

        ride = Ride.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .departureCity("Campobasso")
                .arrivalCity("Pesche")
                .status("OPEN")
                .build();
    }

    @Test
    void getMessagesForRide_whenUserIsDriver_returnsMessages() {
        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .ride(ride)
                .sender(driver)
                .content("Ciao passeggeri!")
                .createdAt(LocalDateTime.now())
                .build();

        MessageResponse responseDto = MessageResponse.builder()
                .id(msg.getId())
                .senderUsername("driver.unimol")
                .content("Ciao passeggeri!")
                .build();

        when(userRepository.findByUsername(driver.getUsername())).thenReturn(Optional.of(driver));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(messageRepository.findByRideOrderByCreatedAtAsc(ride)).thenReturn(List.of(msg));
        when(messageMapper.toResponse(msg)).thenReturn(responseDto);

        List<MessageResponse> result = messageService.getMessagesForRide(driver.getUsername(), ride.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Ciao passeggeri!");
    }

    @Test
    void getMessagesForRide_whenUserIsPassengerAndConfirmed_returnsMessages() {
        when(userRepository.findByUsername(passenger.getUsername())).thenReturn(Optional.of(passenger));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRideAndPassengerAndStatus(ride, passenger, "CONFIRMED")).thenReturn(true);
        when(messageRepository.findByRideOrderByCreatedAtAsc(ride)).thenReturn(List.of());

        List<MessageResponse> result = messageService.getMessagesForRide(passenger.getUsername(), ride.getId());

        assertThat(result).isEmpty();
        verify(messageRepository).findByRideOrderByCreatedAtAsc(ride);
    }

    @Test
    void getMessagesForRide_whenUserIsNotDriverOrPassenger_throwsUnauthorizedException() {
        when(userRepository.findByUsername(externalUser.getUsername())).thenReturn(Optional.of(externalUser));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRideAndPassengerAndStatus(ride, externalUser, "CONFIRMED")).thenReturn(false);

        assertThatThrownBy(() -> messageService.getMessagesForRide(externalUser.getUsername(), ride.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Non hai accesso alla chat di questa corsa");
    }

    @Test
    void sendMessage_whenUserIsPassengerAndConfirmed_savesAndReturnsMessage() {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setContent("Ci vediamo lì!");

        MessageResponse responseDto = MessageResponse.builder()
                .content("Ci vediamo lì!")
                .senderUsername("passenger.unimol")
                .build();

        when(userRepository.findByUsername(passenger.getUsername())).thenReturn(Optional.of(passenger));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRideAndPassengerAndStatus(ride, passenger, "CONFIRMED")).thenReturn(true);
        when(messageMapper.toResponse(any(Message.class))).thenReturn(responseDto);

        MessageResponse result = messageService.sendMessage(passenger.getUsername(), ride.getId(), request);

        assertThat(result.getContent()).isEqualTo("Ci vediamo lì!");
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessage_whenRideIsCompleted_throwsInvalidRequestException() {
        ride.setStatus("COMPLETED");
        CreateMessageRequest request = new CreateMessageRequest();
        request.setContent("Ci vediamo lì!");

        when(userRepository.findByUsername(driver.getUsername())).thenReturn(Optional.of(driver));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> messageService.sendMessage(driver.getUsername(), ride.getId(), request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Non puoi inviare messaggi in una corsa già terminata");
    }

    @Test
    void sendMessage_whenUserIsNotDriverOrPassenger_throwsUnauthorizedException() {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setContent("Spam!");

        when(userRepository.findByUsername(externalUser.getUsername())).thenReturn(Optional.of(externalUser));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRideAndPassengerAndStatus(ride, externalUser, "CONFIRMED")).thenReturn(false);

        assertThatThrownBy(() -> messageService.sendMessage(externalUser.getUsername(), ride.getId(), request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Non hai accesso alla chat di questa corsa");
    }
}
