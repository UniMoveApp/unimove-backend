package com.unimove.unimove.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtFilterTest {

    private JwtUtil jwtUtil;
    private JwtFilter jwtFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        jwtFilter = new JwtFilter(jwtUtil);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_senzaHeaderAuthorization_continuaSenzaAutenticare() throws Exception {
        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).isTokenValid(anyString());
    }

    @Test
    void doFilter_headerSenzaPrefissoBearer_continuaSenzaAutenticare() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_tokenNonValido_nonAutentica() throws Exception {
        request.addHeader("Authorization", "Bearer token.non.valido");
        when(jwtUtil.isTokenValid("token.non.valido")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_tokenValido_popolaSecurityContext() throws Exception {
        request.addHeader("Authorization", "Bearer token.valido");
        when(jwtUtil.isTokenValid("token.valido")).thenReturn(true);
        when(jwtUtil.extractUsername("token.valido")).thenReturn("l.lanese");

        jwtFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isEqualTo("l.lanese");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void doFilter_continuaSempreLaCatenaDiFiltri() throws Exception {
        request.addHeader("Authorization", "Bearer token.valido");
        when(jwtUtil.isTokenValid("token.valido")).thenReturn(true);
        when(jwtUtil.extractUsername("token.valido")).thenReturn("l.lanese");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(filterChain.getRequest()).isNotNull();
    }
}