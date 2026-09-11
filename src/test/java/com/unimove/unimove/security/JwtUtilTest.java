package com.unimove.unimove.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "3d8f2b1c9e4a7f6d0b5e8c2a1f4d7e0b3c6a9f2d5e8b1c4a7f0d3e6b9c2a5f8");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 864000000L);
    }

    @Test
    void generateToken_creaTokenValido() {
        String token = jwtUtil.generateToken("l.lanese", "STUDENT");
        assertThat(token).isNotNull();
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("l.lanese");
    }
}
