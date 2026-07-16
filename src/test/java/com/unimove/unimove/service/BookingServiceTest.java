package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.CreateBookingRequest;
import com.unimove.unimove.dto.response.BookingResponse;
import com.unimove.unimove.exception.InvalidRequestException;
import com.unimove.unimove.exception.ResourceNotFoundException;
import com.unimove.unimove.model.Booking;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.Role;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.BookingRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private NotificationService notificationService;

    private BookingService bookingService;

    private User passenger;
    private User driver;
    private Ride ride;
    private CreateBookingRequest request;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, rideRepository, userRepository, bookingMapper, notificationService);

        driver = User.builder().id(UUID.randomUUID()).username("l.lanese").role(Role.STUDENT).build();

        passenger = User.builder().id(UUID.randomUUID()).username("test.passeggero").role(Role.STUDENT).build();

        ride = Ride.builder().id(UUID.randomUUID()).driver(driver).status("OPEN").availableSeats(3).departureTime(LocalDateTime.now().plusDays(5)).build();

        request = new CreateBookingRequest();
        request.setRideId(ride.getId());
        request.setHotspotChosen("Stazione Centrale");
    }

    @Test
    void createBooking_prenotazioneValida_creaBookingConSuccesso() {
        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(passenger));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRideAndPassenger(ride, passenger)).thenReturn(false);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(BookingResponse.builder().build());

        BookingResponse response = bookingService.createBooking("test.passeggero", request);

        assertThat(response).isNotNull();
        verify(bookingRepository).save(argThat(booking -> booking.getPassenger().equals(passenger) && booking.getRide().equals(ride) && booking.getHotspotChosen().equals("Stazione Centrale")));
    }

    @Test
    void createBooking_corsaNonOpen_lanciaEccezione() {
        ride.setStatus("IN_PROGRESS");

        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(passenger));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> bookingService.createBooking("test.passeggero", request)).isInstanceOf(InvalidRequestException.class).hasMessage("Corsa non ancora disponibile per prenotazioni");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_guidatorePrenotaPropriaCorsa_lanciaEccezione() {
        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> bookingService.createBooking("l.lanese", request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Non puoi prenotare la tua stessa corsa");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_doppiaPrenotazione_lanciaEccezione() {
        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(passenger));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRideAndPassenger(ride, passenger)).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking("test.passeggero", request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Hai già prenotato questa corsa");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_nessunPostoDisponibile_lanciaEccezione() {
        ride.setAvailableSeats(0);
        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(passenger));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRideAndPassenger(ride, passenger)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.createBooking("test.passeggero", request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Nessun posto disponibile");
    }

    @Test
    void createBooking_utenteNonTrovato_lanciaEccezione() {
        when(userRepository.findByUsername("inesistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking("inesistente", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Utente non trovato");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_corsaNonTrovata_lanciaEccezione() {
        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(passenger));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking("test.passeggero", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Corsa non trovata");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_cancellazioneValida_eliminaBooking() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .ride(ride)
                .passenger(passenger)
                .status("CONFIRMED")
                .build();

        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findByIdAndPassenger(bookingId, passenger)).thenReturn(Optional.of(booking));

        bookingService.cancelBooking("test.passeggero", bookingId);

        verify(rideRepository).incrementAvailableSeats(ride.getId());
        verify(bookingRepository).delete(booking);
    }

    @Test
    void cancelBooking_menoDi24Ore_lanciaEccezione() {
        ride.setDepartureTime(LocalDateTime.now().plusHours(10));
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .ride(ride)
                .passenger(passenger)
                .build();

        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findByIdAndPassenger(bookingId, passenger)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("test.passeggero", bookingId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Non puoi cancellare una prenotazione con meno di 24 ore dalla partenza");

        verify(bookingRepository, never()).delete(any());
    }

    @Test
    void cancelBooking_corsaInProgress_abbandonaConSuccesso() {
        ride.setStatus("IN_PROGRESS");
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .ride(ride)
                .passenger(passenger)
                .status("CONFIRMED")
                .build();

        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findByIdAndPassenger(bookingId, passenger)).thenReturn(Optional.of(booking));

        bookingService.cancelBooking("test.passeggero", bookingId);

        verify(rideRepository).incrementAvailableSeats(ride.getId());
        verify(bookingRepository).delete(booking);
    }

    @Test
    void cancelBooking_corsaCompleted_lanciaEccezione() {
        ride.setStatus("COMPLETED");
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .ride(ride)
                .passenger(passenger)
                .build();

        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findByIdAndPassenger(bookingId, passenger)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("test.passeggero", bookingId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Non puoi abbandonare una corsa già terminata");

        verify(bookingRepository, never()).delete(any());
    }

    @Test
    void cancelBooking_prenotazioneNonTrovata_lanciaEccezione() {
        UUID bookingId = UUID.randomUUID();

        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findByIdAndPassenger(bookingId, passenger)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking("test.passeggero", bookingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Prenotazione non trovata");
    }

    @Test
    void getMyBookings_passeggeroConPrenotazioni_restituisceListaPopolata() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .ride(ride)
                .passenger(passenger)
                .build();
        BookingResponse bookingResponse = BookingResponse.builder().build();

        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findByPassenger(passenger)).thenReturn(List.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(bookingResponse);

        List<BookingResponse> result = bookingService.getMyBookings("test.passeggero");

        assertThat(result).hasSize(1);
    }

    @Test
    void getMyBookings_passeggeroSenzaPrenotazioni_restituisceListaVuota() {
        when(userRepository.findByUsername("test.passeggero")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findByPassenger(passenger)).thenReturn(List.of());

        List<BookingResponse> result = bookingService.getMyBookings("test.passeggero");

        assertThat(result).isEmpty();
    }

    @Test
    void getBookingsByRide_guidatoreValido_restituiscePrenotazioni() {
        Booking booking = Booking.builder().id(UUID.randomUUID()).ride(ride).passenger(passenger).status("PENDING").build();
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(bookingRepository.findByRide(ride)).thenReturn(List.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(BookingResponse.builder().build());

        List<BookingResponse> result = bookingService.getBookingsByRide("l.lanese", ride.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void getBookingsByRide_nonGuidatore_lanciaEccezione() {
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> bookingService.getBookingsByRide("altro.utente", ride.getId()))
                .isInstanceOf(com.unimove.unimove.exception.UnauthorizedException.class)
                .hasMessage("Non sei il guidatore di questa corsa");
    }

    @Test
    void acceptBooking_prenotazioneValida_accettaConSuccesso() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder().id(bookingId).ride(ride).passenger(passenger).status("PENDING").build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(rideRepository.decrementAvailableSeats(ride.getId())).thenReturn(1);
        when(bookingMapper.toResponse(booking)).thenReturn(BookingResponse.builder().id(bookingId).status("CONFIRMED").build());

        BookingResponse response = bookingService.acceptBooking("l.lanese", bookingId);

        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(booking.getStatus()).isEqualTo("CONFIRMED");
        verify(rideRepository).decrementAvailableSeats(ride.getId());
        verify(bookingRepository).save(booking);
    }

    @Test
    void acceptBooking_nonPendente_lanciaEccezione() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder().id(bookingId).ride(ride).passenger(passenger).status("CONFIRMED").build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.acceptBooking("l.lanese", bookingId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Puoi accettare solo prenotazioni in attesa (PENDING)");
    }

    @Test
    void rejectBooking_prenotazioneValida_rifiutaConSuccesso() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder().id(bookingId).ride(ride).passenger(passenger).status("PENDING").build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(BookingResponse.builder().id(bookingId).status("REJECTED").build());

        BookingResponse response = bookingService.rejectBooking("l.lanese", bookingId);

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(booking.getStatus()).isEqualTo("REJECTED");
        verify(bookingRepository).save(booking);
        verify(rideRepository, never()).decrementAvailableSeats(any());
    }
}
