package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.CreateBookingRequest;
import com.unimove.unimove.dto.response.BookingResponse;
import com.unimove.unimove.exception.InvalidRequestException;
import com.unimove.unimove.exception.ResourceNotFoundException;
import com.unimove.unimove.exception.UnauthorizedException;
import com.unimove.unimove.model.Booking;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.BookingRepository;
import com.unimove.unimove.repository.RideRepository;
import com.unimove.unimove.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final String UTENTE_NON_TROVATO = "Utente non trovato";
    private static final String CORSA_NON_TROVATA = "Corsa non trovata";

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository, RideRepository rideRepository, UserRepository userRepository, BookingMapper bookingMapper, NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.bookingMapper = bookingMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public BookingResponse createBooking(String username, CreateBookingRequest request) {

        User passenger = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(UTENTE_NON_TROVATO));

        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException(CORSA_NON_TROVATA));

        if (!ride.getStatus().equals("OPEN")) {
            throw new InvalidRequestException("Corsa non ancora disponibile per prenotazioni");
        }

        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new InvalidRequestException("Non puoi prenotare la tua stessa corsa");
        }

        if (bookingRepository.existsByRideAndPassenger(ride, passenger)) {
            throw new InvalidRequestException("Hai già prenotato questa corsa");
        }

        if (ride.getAvailableSeats() <= 0) {
            throw new InvalidRequestException("Nessun posto disponibile");
        }

        Booking booking = Booking.builder()
                .ride(ride)
                .passenger(passenger)
                .hotspotChosen(request.getHotspotChosen())
                .build();

        bookingRepository.save(booking);

        // Notify the driver (RF-8.6)
        String msg = String.format("%s richiede di partecipare alla tua corsa da %s a %s (punto d'incontro: %s).",
                passenger.getFullName(),
                ride.getDepartureCity(),
                ride.getArrivalCity(),
                booking.getHotspotChosen() != null ? booking.getHotspotChosen() : "da concordare");
        notificationService.sendNotification(ride.getDriver(), "BOOKING_REQUEST", msg, ride);

        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public void cancelBooking(String username, UUID bookingId) {

        User passenger = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(UTENTE_NON_TROVATO));

        Booking booking = bookingRepository.findByIdAndPassenger(bookingId, passenger)
                .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata"));

        Ride ride = booking.getRide();

        if (ride.getStatus().equals("OPEN")) {
            if (LocalDateTime.now(ZoneId.of("Europe/Rome")).isAfter(ride.getDepartureTime().minusHours(24))) {
                throw new InvalidRequestException("Non puoi cancellare una prenotazione con meno di 24 ore dalla partenza");
            }
        } else if (ride.getStatus().equals("IN_PROGRESS")) {
            //Abbandono corsa in corso consentito senza vincoli temporali
        } else {
            throw new InvalidRequestException("Non puoi abbandonare una corsa già terminata");
        }

        if (booking.getStatus().equals("CONFIRMED")) {
            int oldAvailable = ride.getAvailableSeats();
            rideRepository.incrementAvailableSeats(ride.getId());

            if (oldAvailable == 0) {
                // Find the oldest PENDING booking for this ride (RF-8.4)
                List<Booking> pendingBookings = bookingRepository.findByRide(ride).stream()
                        .filter(b -> b.getStatus().equals("PENDING"))
                        .sorted((b1, b2) -> b1.getCreatedAt().compareTo(b2.getCreatedAt()))
                        .toList();
                if (!pendingBookings.isEmpty()) {
                    Booking firstInLine = pendingBookings.get(0);
                    String notifyMsg = String.format("Si è liberato un posto per la corsa da %s a %s! La tua richiesta è ora in cima alla lista.",
                            ride.getDepartureCity(),
                            ride.getArrivalCity());
                    notificationService.sendNotification(firstInLine.getPassenger(), "SEAT_FREED", notifyMsg, ride);
                }
            }
        }

        bookingRepository.delete(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String username) {

        User passenger = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(UTENTE_NON_TROVATO));

        return bookingRepository.findByPassenger(passenger)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByRide(String driverUsername, UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException(CORSA_NON_TROVATA));

        if (!ride.getDriver().getUsername().equals(driverUsername)) {
            throw new UnauthorizedException("Non sei il guidatore di questa corsa");
        }

        return bookingRepository.findByRide(ride)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse acceptBooking(String driverUsername, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata"));

        Ride ride = booking.getRide();

        if (!ride.getDriver().getUsername().equals(driverUsername)) {
            throw new UnauthorizedException("Non sei il guidatore di questa corsa");
        }

        if (!booking.getStatus().equals("PENDING")) {
            throw new InvalidRequestException("Puoi accettare solo prenotazioni in attesa (PENDING)");
        }

        int updated = rideRepository.decrementAvailableSeats(ride.getId());
        if (updated == 0) {
            throw new InvalidRequestException("Non ci sono più posti disponibili per questa corsa");
        }

        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);

        // Notify the passenger (RF-8.1)
        String msg = String.format("La tua richiesta per la corsa da %s a %s è stata accettata!",
                ride.getDepartureCity(),
                ride.getArrivalCity());
        notificationService.sendNotification(booking.getPassenger(), "BOOKING_ACCEPTED", msg, ride);

        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse rejectBooking(String driverUsername, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata"));

        Ride ride = booking.getRide();

        if (!ride.getDriver().getUsername().equals(driverUsername)) {
            throw new UnauthorizedException("Non sei il guidatore di questa corsa");
        }

        if (!booking.getStatus().equals("PENDING")) {
            throw new InvalidRequestException("Puoi rifiutare solo prenotazioni in attesa (PENDING)");
        }

        booking.setStatus("REJECTED");
        bookingRepository.save(booking);

        // Notify the passenger
        String msg = String.format("La tua richiesta per la corsa da %s a %s è stata rifiutata.",
                ride.getDepartureCity(),
                ride.getArrivalCity());
        notificationService.sendNotification(booking.getPassenger(), "BOOKING_REJECTED", msg, ride);

        return bookingMapper.toResponse(booking);
    }
}
