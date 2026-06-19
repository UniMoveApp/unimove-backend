package com.unimove.unimove.service;

import com.unimove.unimove.dto.request.LoginRequest;
import com.unimove.unimove.dto.response.CinecaLoginResponse;
import com.unimove.unimove.dto.response.LoginResponse;
import com.unimove.unimove.model.Role;
import com.unimove.unimove.model.User;
import com.unimove.unimove.repository.UserRepository;
import com.unimove.unimove.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setUsername("l.lanese");
        loginRequest.setPassword("password123");
    }

    @Test
    void login_credenzialiCorrette_utenteNuovo_creaUtenteERestituisceToken() {
        CinecaLoginResponse.UserInfo userInfo = new CinecaLoginResponse.UserInfo();
        userInfo.setFirstName("Luca");
        userInfo.setLastName("Lanese");
        userInfo.setUserId("l.lanese");
        userInfo.setGrpDes("Studenti");

        CinecaLoginResponse cinecaResponse = new CinecaLoginResponse();
        cinecaResponse.setUser(userInfo);

        mockCinecaCall(cinecaResponse);

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.empty());
        when(jwtUtil.generateToken(eq("l.lanese"), eq("STUDENT"))).thenReturn("fake-jwt-token");

        LoginResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("fake-jwt-token");
        assertThat(response.getUsername()).isEqualTo("l.lanese");
        assertThat(response.getRole()).isEqualTo("STUDENT");
        assertThat(response.getFullName()).isEqualTo("Luca Lanese");

        verify(userRepository).save(argThat(user ->
                user.getUsername().equals("l.lanese") &&
                        user.getEmail().equals("l.lanese@studenti.unimol.it") &&
                        user.getRole() == Role.STUDENT
        ));
    }

    @Test
    void login_utenteEsistente_aggiornaDatiUtente() {
        CinecaLoginResponse.UserInfo userInfo = new CinecaLoginResponse.UserInfo();
        userInfo.setFirstName("Luca");
        userInfo.setLastName("Lanese");
        userInfo.setUserId("l.lanese");
        userInfo.setGrpDes("Studenti");

        CinecaLoginResponse cinecaResponse = new CinecaLoginResponse();
        cinecaResponse.setUser(userInfo);

        mockCinecaCall(cinecaResponse);

        User existingUser = User.builder()
                .username("l.lanese")
                .email("vecchia@studenti.unimol.it")
                .fullName("Nome Vecchio")
                .role(Role.STUDENT)
                .build();

        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("fake-jwt-token");

        LoginResponse response = authService.login(loginRequest);

        assertThat(response.getFullName()).isEqualTo("Luca Lanese");
        verify(userRepository).save(argThat(user ->
                user.getFullName().equals("Luca Lanese") &&
                        user.getEmail().equals("l.lanese@studenti.unimol.it")
        ));
        verify(userRepository, never()).save(argThat(user -> user != existingUser));
    }

    @Test
    void login_credenzialiErrate_lanciaEccezione() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("401 Unauthorized"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Credenziali non valide");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_rispostaCinecaSenzaUtente_lanciaEccezione() {
        CinecaLoginResponse cinecaResponse = new CinecaLoginResponse();
        cinecaResponse.setUser(null);

        mockCinecaCall(cinecaResponse);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Risposta CINECA non valida");
    }

    @Test
    void login_ruoloDocente_vieneMappatoCorrettamente() {
        CinecaLoginResponse.UserInfo userInfo = new CinecaLoginResponse.UserInfo();
        userInfo.setFirstName("Mario");
        userInfo.setLastName("Rossi");
        userInfo.setUserId("m.rossi");
        userInfo.setGrpDes("Docenti");

        CinecaLoginResponse cinecaResponse = new CinecaLoginResponse();
        cinecaResponse.setUser(userInfo);

        mockCinecaCall(cinecaResponse);

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(jwtUtil.generateToken(anyString(), eq("PROFESSOR"))).thenReturn("fake-jwt-token");

        LoginResponse response = authService.login(loginRequest);

        assertThat(response.getRole()).isEqualTo("PROFESSOR");
        verify(userRepository).save(argThat(user ->
                user.getRole() == Role.PROFESSOR &&
                        user.getEmail().equals("l.lanese@docenti.unimol.it")
        ));
    }

    @SuppressWarnings("unchecked")
    private void mockCinecaCall(CinecaLoginResponse responseBody) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(CinecaLoginResponse.class)).thenReturn(responseBody);
    }

    @Test
    void login_ruoloPta_vieneMappatoCorrettamente() {
        CinecaLoginResponse.UserInfo userInfo = new CinecaLoginResponse.UserInfo();
        userInfo.setFirstName("Anna");
        userInfo.setLastName("Verdi");
        userInfo.setUserId("a.verdi");
        userInfo.setGrpDes("PTA");

        CinecaLoginResponse cinecaResponse = new CinecaLoginResponse();
        cinecaResponse.setUser(userInfo);

        mockCinecaCall(cinecaResponse);

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(jwtUtil.generateToken(anyString(), eq("STAFF"))).thenReturn("fake-jwt-token");

        LoginResponse response = authService.login(loginRequest);

        assertThat(response.getRole()).isEqualTo("STAFF");
        verify(userRepository).save(argThat(user -> user.getRole() == Role.STAFF));
    }

    @Test
    void login_ruoloSconosciuto_defaultStudent() {
        CinecaLoginResponse.UserInfo userInfo = new CinecaLoginResponse.UserInfo();
        userInfo.setFirstName("Test");
        userInfo.setLastName("User");
        userInfo.setUserId("test.user");
        userInfo.setGrpDes("RuoloInesistente");

        CinecaLoginResponse cinecaResponse = new CinecaLoginResponse();
        cinecaResponse.setUser(userInfo);

        mockCinecaCall(cinecaResponse);

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(jwtUtil.generateToken(anyString(), eq("STUDENT"))).thenReturn("fake-jwt-token");

        LoginResponse response = authService.login(loginRequest);

        assertThat(response.getRole()).isEqualTo("STUDENT");
    }

    @Test
    void register_usernameEsistente_lanciaEccezione() {
        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username già esistente");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_usernameNuovo_creaUtenteStudent() {
        when(userRepository.findByUsername("l.lanese")).thenReturn(Optional.empty());
        when(jwtUtil.generateToken(eq("l.lanese"), eq("STUDENT"))).thenReturn("fake-jwt-token");

        LoginResponse response = authService.register(loginRequest);

        assertThat(response.getToken()).isEqualTo("fake-jwt-token");
        assertThat(response.getRole()).isEqualTo("STUDENT");
        verify(userRepository).save(argThat(user ->
                user.getRole() == Role.STUDENT &&
                        user.getEmail().equals("l.lanese@studenti.unimol.it") &&
                        user.getPasswordHash() != null
        ));
    }
}