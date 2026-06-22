package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.RoutePreferenceRequest;
import com.unimove.unimove.dto.request.UpdateIbanRequest;
import com.unimove.unimove.dto.request.UpdatePreferenceRequest;
import com.unimove.unimove.dto.response.RideResponse;
import com.unimove.unimove.dto.response.RoutePreferenceResponse;
import com.unimove.unimove.dto.response.UserProfileResponse;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.RoutePreference;
import com.unimove.unimove.model.Role;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.RideRepository;
import com.unimove.unimove.repository.RoutePreferenceRepository;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoutePreferenceRepository routePreferenceRepository;

    @Mock
    private RideMapper rideMapper;

    @Mock
    private RideRepository rideRepository;

    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, routePreferenceRepository, rideMapper, rideRepository);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("l.lanese")
                .email("l.lanese@studenti.unimol.it")
                .fullName("Luca Lanese")
                .role(Role.STUDENT)
                .build();
    }

    @Test
    void getProfile_utenteConCorseImminenti_restituisceProfiloCompleto() {
        Ride ride = Ride.builder()
                .id(UUID.randomUUID())
                .build();
        RideResponse rideResponse = RideResponse.builder()
                .id(ride.getId())
                .departureCity("Campobasso")
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(testUser));
        when(rideRepository.findUpcomingByDriver(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(List.of(ride));
        when(rideMapper.toResponse(ride)).thenReturn(rideResponse);

        UserProfileResponse response = userService.getProfile("l.lanese");

        assertThat(response.getUsername()).isEqualTo("l.lanese");
        assertThat(response.getUpcomingRides()).hasSize(1);
        assertThat(response.getUpcomingRides().getFirst().getDepartureCity()).isEqualTo("Campobasso");
    }

    @Test
    void getProfile_utenteSenzaCorseImminenti_listaVuota() {
        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(testUser));
        when(rideRepository.findUpcomingByDriver(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(List.of());

        UserProfileResponse response = userService.getProfile("l.lanese");

        assertThat(response.getUpcomingRides()).isEmpty();
    }

    @Test
    void getProfile_utenteNonTrovato_lanciaEccezione() {
        when(userRepository.findByUsername("inesistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("inesistente"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato");
    }

    @Test
    void updatePreferences_utenteTrovato_aggiornaPreferenze() {
        UpdatePreferenceRequest request = new UpdatePreferenceRequest();
        request.setTravelPreferences("No fumo, musica ok");

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(testUser));

        userService.updatePreferences("l.lanese", request);

        verify(userRepository).save(argThat(user ->
                user.getTravelPreferences().equals("No fumo, musica ok")));
    }

    @Test
    void updatePreferences_utenteNonTrovato_lanciaEccezione() {
        UpdatePreferenceRequest request = new UpdatePreferenceRequest();
        when(userRepository.findByUsername("inesistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updatePreferences("inesistente", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato");
    }

    @Test
    void updateIban_utenteTrovato_aggiornaIBAN() {
        UpdateIbanRequest request = new UpdateIbanRequest();
        request.setIban("IT1234567890123456789012345");
        request.setIbanHolder("Luca Lanese");

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(testUser));

        userService.updateIban("l.lanese", request);

        verify(userRepository).save(argThat(user ->
                user.getIban().equals("IT1234567890123456789012345") &&
                        user.getIbanHolder().equals("Luca Lanese")));
    }

    @Test
    void updateIban_utenteNonTrovato_lanciaEccezione() {
        UpdateIbanRequest request = new UpdateIbanRequest();
        when(userRepository.findByUsername("inesistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateIban("inesistente", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato");
    }

    @Test
    void getRoutes_utenteConTratte_restituisceListaPopolata() {
        RoutePreference route = RoutePreference.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .cityFrom("Campobasso")
                .cityTo("Roma")
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(testUser));
        when(routePreferenceRepository.findByUser(testUser)).thenReturn(List.of(route));

        List<RoutePreferenceResponse> routes = userService.getRoutes("l.lanese");

        assertThat(routes).hasSize(1);
        assertThat(routes.getFirst().getCityFrom()).isEqualTo("Campobasso");
    }

    @Test
    void getRoutes_utenteSenzaTratte_listaVuota() {
        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(testUser));
        when(routePreferenceRepository.findByUser(testUser)).thenReturn(List.of());

        List<RoutePreferenceResponse> routes = userService.getRoutes("l.lanese");

        assertThat(routes).isEmpty();
    }

    @Test
    void addRoute_sottoLimite_aggiungeTratta() {
        RoutePreferenceRequest request = new RoutePreferenceRequest();
        request.setCityFrom("Campobasso");
        request.setCityTo("Roma");

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(testUser));
        when(routePreferenceRepository.countByUser(testUser)).thenReturn(2);

        RoutePreferenceResponse response = userService.addRoute("l.lanese", request);

        assertThat(response.getCityFrom()).isEqualTo("Campobasso");
        assertThat(response.getCityTo()).isEqualTo("Roma");
        verify(routePreferenceRepository).save(any(RoutePreference.class));
    }

    @Test
    void addRoute_limiteRaggiunto_lanciaEccezione() {
        RoutePreferenceRequest request = new RoutePreferenceRequest();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(testUser));
        when(routePreferenceRepository.countByUser(testUser)).thenReturn(3);

        assertThatThrownBy(() -> userService.addRoute("l.lanese", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Massimo 3 tratte preferite consentite");

        verify(routePreferenceRepository, never()).save(any());
    }

    @Test
    void deleteRoute_trattaValidaEAutorizzata_eliminaConSuccesso() {
        UUID routeId = UUID.randomUUID();
        RoutePreference route = RoutePreference.builder()
                .id(routeId)
                .user(testUser)
                .cityFrom("Campobasso")
                .cityTo("Roma")
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(testUser));
        when(routePreferenceRepository.findById(routeId)).thenReturn(Optional.of(route));

        userService.deleteRoute("l.lanese", routeId);

        verify(routePreferenceRepository).delete(route);
    }

    @Test
    void deleteRoute_trattaDiAltroUtente_lanciaEccezioneNonAutorizzato() {
        UUID routeId = UUID.randomUUID();
        User altroUtente = User.builder()
                .id(UUID.randomUUID())
                .username("altro.utente")
                .build();

        RoutePreference route = RoutePreference.builder()
                .id(routeId)
                .user(altroUtente)
                .cityFrom("Campobasso")
                .cityTo("Roma")
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(testUser));
        when(routePreferenceRepository.findById(routeId)).thenReturn(Optional.of(route));

        assertThatThrownBy(() -> userService.deleteRoute("l.lanese", routeId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Non sei autorizzato a eliminare questa tratta preferita");

        verify(routePreferenceRepository, never()).delete(route);
    }
    @Test
    void deleteRoute_trattaNonTrovata_lanciaEccezione() {
        UUID routeId = UUID.randomUUID();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(testUser));
        when(routePreferenceRepository.findById(routeId)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> userService.deleteRoute("l.lanese", routeId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tratta preferita non trovata");
    }
}
