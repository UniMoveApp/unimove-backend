package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.CreateRideRequest;
import com.unimove.unimove.dto.response.RideResponse;
import com.unimove.unimove.exception.InvalidRequestException;
import com.unimove.unimove.exception.ResourceNotFoundException;
import com.unimove.unimove.exception.UnauthorizedException;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.Role;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.RideRepository;
import com.unimove.unimove.repository.UserRepository;
import com.unimove.unimove.repository.MessageRepository;
import com.unimove.unimove.repository.BookingRepository;
import com.unimove.unimove.repository.RoutePreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RideMapper rideMapper;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoutePreferenceRepository routePreferenceRepository;

    @Mock
    private NotificationService notificationService;

    private RideService rideService;

    private User driver;

    @BeforeEach
    void setUp() {
        rideService = new RideService(
                rideRepository,
                userRepository,
                rideMapper,
                messageRepository,
                bookingRepository,
                routePreferenceRepository,
                notificationService
        );

        driver = User.builder()
                .id(UUID.randomUUID())
                .username("l.lanese")
                .fullName("Luca Lanese")
                .role(Role.STUDENT)
                .build();
    }

    @Test
    void createRide_conTuttiICampi_creaCorsaConSuccesso() {
        CreateRideRequest request = new CreateRideRequest();
        request.setDepartureCity("Campobasso");
        request.setDepartureTime(LocalDateTime.now().plusDays(1));
        request.setArrivalCity("Roma");
        request.setHotspots(List.of("Stazione FS", "Piazza Vittoria"));
        request.setVehicleModel("Fiat Panda");
        request.setVehiclePlate("ABC123CD");
        request.setTotalSeats(4);
        request.setTravelPreferences("No fumo");

        RideResponse exceptedResponse = RideResponse.builder()
                .departureCity("Campobasso")
                .arrivalCity("Roma")
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(java.util.Optional.of(driver));
        when(rideMapper.toResponse(any(Ride.class))).thenReturn(exceptedResponse);

        RideResponse response = rideService.createRide("l.lanese", request);

        assertThat(response.getDepartureCity()).isEqualTo("Campobasso");
        assertThat(response.getArrivalCity()).isEqualTo("Roma");

        verify(rideRepository).save(argThat(ride ->
                ride.getDriver().equals(driver) &&
                        ride.getHotspot1().equals("Stazione FS") &&
                        ride.getHotspot2().equals("Piazza Vittoria") &&
                        ride.getHotspot3() == null &&
                        ride.getTotalSeats() == 4 &&
                        ride.getAvailableSeats() == 4
        ));
    }

    @Test
    void createRide_senzaCampiOpzionali_creaCorsaConSuccesso() {
        CreateRideRequest request = new CreateRideRequest();
        request.setDepartureCity("Isernia");
        request.setDepartureTime(LocalDateTime.now().plusDays(1));
        request.setArrivalCity("Napoli");
        request.setTotalSeats(3);

        RideResponse expectedResponse = RideResponse.builder()
                .departureCity("Isernia")
                .arrivalCity("Napoli")
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideMapper.toResponse(any(Ride.class))).thenReturn(expectedResponse);

        RideResponse response = rideService.createRide("l.lanese", request);

        assertThat(response).isNotNull();
        verify(rideRepository).save(argThat(ride ->
                ride.getHotspot1() == null &&
                        ride.getHotspot2() == null &&
                        ride.getHotspot3() == null &&
                        ride.getVehicleModel() == null
        ));
    }

    @Test
    void createRide_utenteNonTrovato_lanciaEccezione() {
        CreateRideRequest request = new CreateRideRequest();

        when(userRepository.findByUsername("inesistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.createRide("inesistente", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Utente non trovato");

        verify(rideRepository, never()).save(any());
    }

    @Test
    void searchRide_senzaFiltri_restituisceTutteLeCorse() {
        Ride ride1 = Ride.builder().id(UUID.randomUUID()).build();
        Ride ride2 = Ride.builder().id(UUID.randomUUID()).build();

        when(rideRepository.search(null, null, null, null))
                .thenReturn(List.of(ride1, ride2));
        when(rideMapper.toResponse(any(Ride.class)))
                .thenReturn(RideResponse.builder().build());

        List<RideResponse> results = rideService.searchRide(null, null, null, null);

        assertThat(results).hasSize(2);
    }

    @Test
    void searchRide_conFiltroCitta_restituisceCorseFiltrate() {
        Ride ride = Ride.builder().id(UUID.randomUUID()).build();
        RideResponse response = RideResponse.builder().departureCity("Campobasso").build();

        when(rideRepository.search(null, "Campobasso", null, null))
                .thenReturn(List.of(ride));
        when(rideMapper.toResponse(any(Ride.class))).thenReturn(response);

        List<RideResponse> results = rideService.searchRide(null, "Campobasso", null, null);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getDepartureCity()).isEqualTo("Campobasso");
    }

    @Test
    void searchRide_conFiltriCombinati_passaTuttiIParametriAlRepository() {
        LocalDate date = LocalDate.of(2026, Month.JULY, 1);

        when(rideRepository.search("l.lanese", "Campobasso", "Roma", date))
                .thenReturn(List.of());

        rideService.searchRide("l.lanese", "Campobasso", "Roma", date);

        verify(rideRepository).search("l.lanese", "Campobasso", "Roma", date);
    }

    @Test
    void searchRide_nessunRisultato_restituisceListaVuota() {
        when(rideRepository.search(any(), any(), any(), any())).thenReturn(List.of());

        List<RideResponse> results = rideService.searchRide("x", "y", "z", LocalDate.now());

        assertThat(results).isEmpty();
    }

    @Test
    void startRide_corsaOpen_cambiaStatoInProgress() {
        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .status("OPEN")
                .build();

        RideResponse expectedResponse = RideResponse.builder()
                .status("IN_PROGRESS")
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(rideMapper.toResponse(ride)).thenReturn(expectedResponse);

        RideResponse response = rideService.startRide("l.lanese", ride.getId());

        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        verify(rideRepository).save(argThat(r -> r.getStatus().equals("IN_PROGRESS")));
    }

    @Test
    void startRide_corsaNonOpen_lanciaEccezione() {
        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .status("CLOSED")
                .build();

        UUID rideId = ride.getId();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.startRide("l.lanese", rideId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Corsa non ancora disponibile per partenza");

        verify(rideRepository, never()).save(any());
    }

    @Test
    void startRide_noGuidatore_lanciaEccezione() {
        User altroUtente = User.builder()
                .id(UUID.randomUUID())
                .username("altro.utente")
                .build();

        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .status("OPEN")
                .build();

        UUID rideId = ride.getId();

        when(userRepository.findByUsername("altro.utente")).thenReturn(Optional.of(altroUtente));
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.startRide("altro.utente", rideId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Non sei il guidatore di questa corsa");

        verify(rideRepository, never()).save(any());
    }

    @Test
    void completeRide_corsaInProgress_cambiaStatoCompleted() {
        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .status("IN_PROGRESS")
                .build();

        RideResponse expectedResponse = RideResponse.builder()
                .status("COMPLETED")
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(rideMapper.toResponse(ride)).thenReturn(expectedResponse);

        RideResponse response = rideService.completeRide("l.lanese", ride.getId());

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(rideRepository).save(argThat(r -> r.getStatus().equals("COMPLETED")));
    }

    @Test
    void completeRide_corsaNonInProgress_lanciaEccezione() {
        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .status("OPEN")
                .build();

        UUID rideId = ride.getId();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.completeRide("l.lanese", rideId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Corsa non ancora in corso");

        verify(rideRepository, never()).save(any());
    }

    @Test
    void deleteRide_conPreavvisoSufficiente_eliminaCorsa() {
        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .status("OPEN")
                .departureTime(LocalDateTime.now().plusDays(5))
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));

        rideService.deleteRide("l.lanese", ride.getId());

        verify(rideRepository).delete(ride);
    }

    @Test
    void deleteRide_senzaPreavvisoSufficiente_lanciaEccezione() {
        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .status("OPEN")
                .departureTime(LocalDateTime.now().plusHours(10))
                .build();

        UUID rideId = ride.getId();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.deleteRide("l.lanese", rideId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Non puoi cancellare una corsa con meno di 48 ore dalla partenza");

        verify(rideRepository, never()).delete(any());
    }

    @Test
    void deleteRide_corsaNonOpen_lanciaEccezione() {
        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .status("IN_PROGRESS")
                .departureTime(LocalDateTime.now().plusDays(5))
                .build();

        UUID rideId = ride.getId();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findById(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.deleteRide("l.lanese", rideId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Corsa non ancora disponibile per cancellazione");

        verify(rideRepository, never()).delete(any());
    }

    @Test
    void getMyRides_conFiltroStato_restituisceCorseFiltrate() {
        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .build();
        RideResponse response = RideResponse.builder()
                .status("OPEN")
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findByDriverAndStatus(driver, "OPEN")).thenReturn(List.of(ride));
        when(rideMapper.toResponse(ride)).thenReturn(response);

        List<RideResponse> results = rideService.getMyRides("l.lanese", "OPEN");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getStatus()).isEqualTo("OPEN");
    }

    @Test
    void getMyRides_senzaFiltro_restituisceTutteLeCorse() {
        Ride ride1 = Ride.builder().id(UUID.randomUUID()).build();
        Ride ride2 = Ride.builder().id(UUID.randomUUID()).build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findByDriverAndStatus(driver, null)).thenReturn(List.of(ride1, ride2));
        when(rideMapper.toResponse(any())).thenReturn(RideResponse.builder().build());

        List<RideResponse> results = rideService.getMyRides("l.lanese", null);

        assertThat(results).hasSize(2);
    }

    @Test
    void getArchive_utenteConCorseComeGuidatore_restituisceArchivio() {
        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .build();
        RideResponse response = RideResponse.builder()
                .id(UUID.randomUUID())
                .status("COMPLETED")
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findCompletedByDriver(driver)).thenReturn(List.of(ride));
        when(rideRepository.findCompletedByPassenger(driver)).thenReturn(List.of());
        when(rideMapper.toResponse(ride)).thenReturn(response);

        List<RideResponse> results = rideService.getArchive("l.lanese");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void getArchive_utenteConCorseComePasseggero_restituisceArchivio() {
        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .build();
        RideResponse response = RideResponse.builder()
                .id(UUID.randomUUID())
                .status("COMPLETED")
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findCompletedByDriver(driver)).thenReturn(List.of());
        when(rideRepository.findCompletedByPassenger(driver)).thenReturn(List.of(ride));
        when(rideMapper.toResponse(ride)).thenReturn(response);

        List<RideResponse> results = rideService.getArchive("l.lanese");

        assertThat(results).hasSize(1);
    }

    @Test
    void getArchive_utenteConCorseDuplicati_restituisceSenzaDuplicati() {
        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .build();
        RideResponse response = RideResponse.builder()
                .id(UUID.randomUUID())
                .status("COMPLETED")
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(driver));
        when(rideRepository.findCompletedByDriver(driver)).thenReturn(List.of(ride));
        when(rideRepository.findCompletedByPassenger(driver)).thenReturn(List.of(ride));
        when(rideMapper.toResponse(ride)).thenReturn(response);

        List<RideResponse> results = rideService.getArchive("l.lanese");

        assertThat(results).hasSize(1);
    }



}
