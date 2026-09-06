package com.codelens.service;

import com.codelens.dto.AuthResponse;
import com.codelens.dto.LoginRequest;
import com.codelens.dto.RegisterRequest;
import com.codelens.entity.User;
import com.codelens.entity.enums.UserRole;
import com.codelens.repository.UserRepository;
import com.codelens.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("test@codelens.ai")
                .password("password123")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@codelens.ai")
                .password("password123")
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@codelens.ai")
                .password("encoded_password")
                .role(UserRole.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("Register user successfully")
    void testRegisterSuccess() {
        when(userRepository.existsByEmail("test@codelens.ai")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateToken("test@codelens.ai")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals("test@codelens.ai", response.getEmail());
        assertEquals(UserRole.ROLE_USER, response.getRole());

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register fails when email is already registered")
    void testRegisterDuplicateEmail() {
        when(userRepository.existsByEmail("test@codelens.ai")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals("Email is already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login user successfully")
    void testLoginSuccess() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("test@codelens.ai")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("test@codelens.ai")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("test@codelens.ai", response.getEmail());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Login fails when authentication manager throws exception")
    void testLoginInvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }
}
