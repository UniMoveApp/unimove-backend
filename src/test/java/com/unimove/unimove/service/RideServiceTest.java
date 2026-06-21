package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.CreateRideRequest;
import com.unimove.unimove.dto.response.RideResponse;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.Role;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.RideRepository;
import com.unimove.unimove.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RideMapper rideMapper;

    private RideService rideService;

    private User driver;

    @BeforeEach
    void setUp() {
        rideService = new RideService(rideRepository, userRepository, rideMapper);

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
                .isInstanceOf(RuntimeException.class)
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
        assertThat(results.get(0).getDepartureCity()).isEqualTo("Campobasso");
    }

    @Test
    void searchRide_conFiltriCombinati_passaTuttiIParametriAlRepository() {
        LocalDate date = LocalDate.of(2026, 7, 1);

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

}
