package com.codelens.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTests {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L); // 1 hour
    }

    @Test
    @DisplayName("Generate token and extract email successfully")
    void testGenerateAndExtractEmail() {
        String email = "dev@codelens.ai";
        String token = jwtUtil.generateToken(email);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String extractedEmail = jwtUtil.extractEmail(token);
        assertEquals(email, extractedEmail);
    }

    @Test
    @DisplayName("Validate token against matching UserDetails")
    void testValidateTokenSuccess() {
        String email = "user@codelens.ai";
        String token = jwtUtil.generateToken(email);

        UserDetails userDetails = new User(email, "password", Collections.emptyList());

        assertTrue(jwtUtil.validateToken(token, userDetails));
        assertTrue(jwtUtil.validateToken(token));
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    @DisplayName("Reject invalid token")
    void testValidateInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.jwt.token"));
    }
}
