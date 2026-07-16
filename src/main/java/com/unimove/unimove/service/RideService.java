package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.CreateRideRequest;
import com.unimove.unimove.dto.response.RideResponse;
import com.unimove.unimove.exception.InvalidRequestException;
import com.unimove.unimove.exception.ResourceNotFoundException;
import com.unimove.unimove.exception.UnauthorizedException;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.User;
import com.unimove.unimove.model.Booking;
import com.unimove.unimove.model.RoutePreference;
import com.unimove.unimove.repository.BookingRepository;
import com.unimove.unimove.repository.RoutePreferenceRepository;
import com.unimove.unimove.repository.MessageRepository;
import com.unimove.unimove.repository.RideRepository;
import com.unimove.unimove.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class RideService {

    private static final String UTENTE_NON_TROVATO = "Utente non trovato";
    private static final String CORSA_NON_TROVATA = "Corsa non trovata";
    private static final String NON_SEI_IL_GUIDATORE = "Non sei il guidatore di questa corsa";

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final RideMapper rideMapper;
    private final MessageRepository messageRepository;
    private final BookingRepository bookingRepository;
    private final RoutePreferenceRepository routePreferenceRepository;
    private final NotificationService notificationService;

    public RideService(RideRepository rideRepository,
                       UserRepository userRepository,
                       RideMapper rideMapper,
                       MessageRepository messageRepository,
                       BookingRepository bookingRepository,
                       RoutePreferenceRepository routePreferenceRepository,
                       NotificationService notificationService) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.rideMapper = rideMapper;
        this.messageRepository = messageRepository;
        this.bookingRepository = bookingRepository;
        this.routePreferenceRepository = routePreferenceRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public RideResponse createRide(String username, CreateRideRequest request) {

        User driver = getUser(username);

        List<String> hotspots = request.getHotspots();

        Ride ride = Ride.builder()
                .driver(driver)
                .departureCity(request.getDepartureCity())
                .departureTime(request.getDepartureTime())
                .arrivalCity(request.getArrivalCity())
                .arrivalTimeEst(request.getArrivalTimeEst())
                .hotspot1(getHotspot(hotspots, 0))
                .hotspot2(getHotspot(hotspots, 1))
                .hotspot3(getHotspot(hotspots, 2))
                .vehicleModel(request.getVehicleModel())
                .vehiclePlate(request.getVehiclePlate())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .travelPreferences(request.getTravelPreferences())
                .build();

        rideRepository.save(ride);

        // Notify users with matching route preferences (RF-8.5)
        List<RoutePreference> matchingPrefs = routePreferenceRepository.findByCityFromIgnoreCaseAndCityToIgnoreCase(
                ride.getDepartureCity(),
                ride.getArrivalCity()
        );
        for (RoutePreference pref : matchingPrefs) {
            if (!pref.getUser().getId().equals(driver.getId())) {
                String notifyMsg = String.format("Nuova corsa disponibile sulla tua tratta preferita %s → %s creata da %s.",
                        ride.getDepartureCity(),
                        ride.getArrivalCity(),
                        driver.getFullName());
                notificationService.sendNotification(pref.getUser(), "NEW_RIDE_AVAILABLE", notifyMsg, ride);
            }
        }

        return rideMapper.toResponse(ride);
    }

    @Transactional(readOnly = true)
    public List<RideResponse> searchRide(String driverUsername, String departureCity, String arrivalCity, LocalDate date) {
        return rideRepository.search(driverUsername, departureCity, arrivalCity, date)
                .stream()
                .map(rideMapper::toResponse)
                .toList();
    }

    @Transactional
    public RideResponse startRide(String username, UUID rideId) {

        User driver = getUser(username);
        Ride ride = getVerifiedRide(rideId, driver);

        if (!ride.getStatus().equals("OPEN")) {
            throw new InvalidRequestException("Corsa non ancora disponibile per partenza");
        }

        ride.setStatus("IN_PROGRESS");
        rideRepository.save(ride);

        return rideMapper.toResponse(ride);
    }

    @Transactional
    public RideResponse completeRide(String username, UUID rideId) {

        User driver = getUser(username);
        Ride ride = getVerifiedRide(rideId, driver);

        if (!ride.getStatus().equals("IN_PROGRESS")) {
            throw new InvalidRequestException("Corsa non ancora in corso");
        }

        ride.setStatus("COMPLETED");
        rideRepository.save(ride);

        // Delete chat messages upon completion to comply with GDPR
        messageRepository.deleteByRide(ride);

        return rideMapper.toResponse(ride);
    }

    @Transactional
    public void deleteRide(String username, UUID rideId) {

        User driver = getUser(username);
        Ride ride = getVerifiedRide(rideId, driver);

        if (!ride.getStatus().equals("OPEN")) {
            throw new InvalidRequestException("Corsa non ancora disponibile per cancellazione");
        }

        if (LocalDateTime.now(ZoneId.of("Europe/Rome")).isAfter(ride.getDepartureTime().minusHours(48))) {
            throw new InvalidRequestException("Non puoi cancellare una corsa con meno di 48 ore dalla partenza");
        }

        // Notify all booked passengers (RF-8.2)
        List<Booking> bookings = bookingRepository.findByRide(ride);
        for (Booking booking : bookings) {
            String notifyMsg = String.format("La corsa da %s a %s del %s a cui eri prenotato è stata annullata dal guidatore.",
                    ride.getDepartureCity(),
                    ride.getArrivalCity(),
                    ride.getDepartureTime());
            notificationService.sendNotification(booking.getPassenger(), "RIDE_CANCELLED", notifyMsg, null);
        }

        rideRepository.delete(ride);
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getMyRides(String username, String status) {

        User driver = getUser(username);

        return rideRepository.findByDriverAndStatus(driver, status)
                .stream()
                .map(rideMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getArchive(String username) {

        User user = getUser(username);

        List<RideResponse> asDriver = rideRepository.findCompletedByDriver(user)
                .stream()
                .map(rideMapper::toResponse)
                .toList();

        List<RideResponse> asPassenger = rideRepository.findCompletedByPassenger(user)
                .stream()
                .map(rideMapper::toResponse)
                .toList();

        return Stream.concat(asDriver.stream(), asPassenger.stream())
                .distinct()
                .toList();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(UTENTE_NON_TROVATO));
    }

    private Ride getVerifiedRide(UUID rideId, User driver) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException(CORSA_NON_TROVATA));

        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException(NON_SEI_IL_GUIDATORE);
        }

        return ride;
    }

    private String getHotspot(List<String> hotspots, int index) {
        if (hotspots == null || hotspots.size() <= index) {
            return null;
        }
        return hotspots.get(index);
    }
}