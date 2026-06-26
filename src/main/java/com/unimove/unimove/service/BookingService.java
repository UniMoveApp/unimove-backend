package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.CreateBookingRequest;
import com.unimove.unimove.dto.response.BookingResponse;
import com.unimove.unimove.model.Booking;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.BookingRepository;
import com.unimove.unimove.repository.RideRepository;
import com.unimove.unimove.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    public BookingService(BookingRepository bookingRepository, RideRepository rideRepository, UserRepository userRepository, BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public BookingResponse createBooking(String username, CreateBookingRequest request) {

        User passenger = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new RuntimeException("Corsa non trovato"));

        if (!ride.getStatus().equals("OPEN")) {
            throw new RuntimeException("Corsa non ancora disponibile per prenotazioni");
        }


        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new RuntimeException("Non puoi prenotare la tua stessa corsa");
        }

        if (bookingRepository.existsByRideAndPassenger(ride, passenger)) {
            throw new RuntimeException("Hai già prenotato questa corsa");
        }

        Booking booking = Booking.builder()
                .ride(ride)
                .passenger(passenger)
                .hotspotChosen(request.getHotspotChosen())
                .build();

        bookingRepository.save(booking);

        int updated = rideRepository.decrementAvailableSeats(ride.getId());
        if (updated == 0) {
            throw new RuntimeException("Nessun posto disponibile");
        }

        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public void cancelBooking(String username, UUID bookingId) {

        User passenger = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        Booking booking = bookingRepository.findByIdAndPassenger(bookingId, passenger)
                .orElseThrow(() -> new RuntimeException("Prenotazione non trovata"));

        Ride ride = booking.getRide();

        if (ride.getStatus().equals("OPEN")) {
            if (LocalDateTime.now().isAfter(ride.getDepartureTime().minusHours(24))) {
                throw new RuntimeException("Non puoi cancellare una prenotazione con meno di 24 ore dalla partenza");
            }
        } else if (ride.getStatus().equals("IN_PROGRESS")) {
        } else {
            throw new RuntimeException("Non puoi abbandonare una corsa già terminata");
        }

        rideRepository.incrementAvailableSeats(ride.getId());

        bookingRepository.delete(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String username) {

        User passenger = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        return bookingRepository.findByPassenger(passenger)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }
}
