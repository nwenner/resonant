package com.wenroe.resonant.service;

import com.wenroe.resonant.dto.AuthResponse;
import com.wenroe.resonant.dto.LoginRequest;
import com.wenroe.resonant.dto.RegisterRequest;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.UserRole;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        // Setup register request
        registerRequest = new RegisterRequest();
        registerRequest.setName("Test User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        // Setup login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        // Setup test user
        testUser = new User();
        testUser.setId(userId);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$10$hashedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
    }

    @Test
    @DisplayName("Should register new user successfully")
    void register_Success() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token-12345");

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-12345");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getName()).isEqualTo("Test User");
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        // Verify interactions
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken(any(User.class));
    }

    @Test
    @DisplayName("Should hash password when registering")
    void register_PasswordIsHashed() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        // When
        authService.register(registerRequest);

        // Then - Capture the user being saved
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$10$hashedPassword");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("password123"); // Plain text not stored

        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("Should set default role and enabled flag on registration")
    void register_DefaultValues() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        // When
        authService.register(registerRequest);

        // Then - Verify default values
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when registering with duplicate email")
    void register_DuplicateEmail() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already registered");

        // Verify password was never encoded and user was never saved
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should login with valid credentials")
    void login_Success() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(jwtUtil.generateToken(testUser)).thenReturn("jwt-token-67890");

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-67890");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getName()).isEqualTo("Test User");
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        // Verify authentication attempt
        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authCaptor.capture());

        UsernamePasswordAuthenticationToken authToken = authCaptor.getValue();
        assertThat(authToken.getPrincipal()).isEqualTo("test@example.com");
        assertThat(authToken.getCredentials()).isEqualTo("password123");
    }

    @Test
    @DisplayName("Should throw exception when login with invalid credentials")
    void login_InvalidCredentials() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        // Verify JWT was never generated
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should throw exception when login with non-existent user")
    void login_UserNotFound() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("User not found"));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should generate JWT token after successful registration")
    void register_GeneratesJwtToken() {
        // Given
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("generated-jwt-token");

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getToken()).isEqualTo("generated-jwt-token");

        verify(jwtUtil).generateToken(testUser);
    }

    @Test
    @DisplayName("Should generate JWT token after successful login")
    void login_GeneratesJwtToken() {
        // Given
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(jwtUtil.generateToken(testUser)).thenReturn("login-jwt-token");

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getToken()).isEqualTo("login-jwt-token");

        verify(jwtUtil).generateToken(testUser);
    }

    @Test
    @DisplayName("Should return user details in auth response on registration")
    void register_ReturnsUserDetails() {
        // Given
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any())).thenReturn("token");

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo(testUser.getName());
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should return user details in auth response on login")
    void login_ReturnsUserDetails() {
        // Given
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(jwtUtil.generateToken(any())).thenReturn("token");

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertThat(response.getId()).isEqualTo(testUser.getId());
        assertThat(response.getName()).isEqualTo(testUser.getName());
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should use authentication manager for login")
    void login_UsesAuthenticationManager() {
        // Given
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(jwtUtil.generateToken(any())).thenReturn("token");

        // When
        authService.login(loginRequest);

        // Then
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}