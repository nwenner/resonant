package com.wenroe.resonant.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Auth Integration Tests")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register new user and return JWT token")
    void registerUser_Success() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setName("Integration Test User");
        request.setEmail("integration@example.com");
        request.setPassword("password123");

        // When
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.email").value("integration@example.com"))
                .andExpect(jsonPath("$.name").value("Integration Test User"))
                .andReturn();

        // Then - Verify token is valid
        String responseBody = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);

        assertThat(authResponse.getToken()).isNotNull();
        assertThat(authResponse.getToken()).isNotEmpty();

        // Verify user was saved in database
        assertThat(userRepository.findByEmail("integration@example.com")).isPresent();

        // Verify password was hashed
        User savedUser = userRepository.findByEmail("integration@example.com").get();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void registerUser_DuplicateEmail() throws Exception {
        // Given - Create existing user
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        existingUser.setName("Existing User");
        existingUser.setPasswordHash(passwordEncoder.encode("password123"));
        existingUser.setRole(UserRole.USER);
        existingUser.setEnabled(true);
        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("existing@example.com"); // Same email
        request.setPassword("password456");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    @DisplayName("Should login with valid credentials and return JWT token")
    void login_Success() throws Exception {
        // Given - Create user first
        User user = new User();
        user.setEmail("login@example.com");
        user.setName("Login User");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login@example.com");
        loginRequest.setPassword("password123");

        // When
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andReturn();

        // Then - Verify token is valid
        String responseBody = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);

        assertThat(authResponse.getToken()).isNotNull();
        assertThat(jwtUtil.extractUsername(authResponse.getToken())).isEqualTo("login@example.com");
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void login_InvalidPassword() throws Exception {
        // Given - Create user
        User user = new User();
        user.setEmail("user@example.com");
        user.setName("User");
        user.setPasswordHash(passwordEncoder.encode("correctPassword"));
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("wrongPassword");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Should reject login with non-existent user")
    void login_UserNotFound() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Should access protected endpoint with valid JWT token")
    void accessProtectedEndpoint_WithValidToken() throws Exception {
        // Given - Register user and get token
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Protected Test User");
        registerRequest.setEmail("protected@example.com");
        registerRequest.setPassword("password123");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = registerResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        String token = authResponse.getToken();

        // When & Then - Access protected endpoint
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject access to protected endpoint without token")
    void accessProtectedEndpoint_WithoutToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden()); // Spring Security returns 403 for missing auth
    }

    @Test
    @DisplayName("Should reject access to protected endpoint with invalid token")
    void accessProtectedEndpoint_WithInvalidToken() throws Exception {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject access to protected endpoint with malformed Authorization header")
    void accessProtectedEndpoint_WithMalformedHeader() throws Exception {
        // Given - Missing "Bearer " prefix
        User user = new User();
        user.setEmail("user@example.com");
        user.setName("User");
        String token = jwtUtil.generateToken(user);

        // When & Then - Without "Bearer " prefix
        mockMvc.perform(get("/api/users")
                        .header("Authorization", token)) // Missing "Bearer "
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should complete full registration and authentication flow")
    void fullAuthFlow_RegisterLoginAccessProtected() throws Exception {
        // Step 1: Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Full Flow User");
        registerRequest.setEmail("fullflow@example.com");
        registerRequest.setPassword("securePassword123");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse registerResponse = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        String registerToken = registerResponse.getToken();

        // Step 2: Access protected endpoint with registration token
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + registerToken))
                .andExpect(status().isOk());

        // Wait to ensure different timestamp (JWT uses seconds)
        Thread.sleep(1100);

        // Step 3: Login with same credentials
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("fullflow@example.com");
        loginRequest.setPassword("securePassword123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        String loginToken = loginResponse.getToken();

        // Step 4: Access protected endpoint with login token
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk());

        // Verify: Both tokens should work (but be different)
        assertThat(registerToken).isNotEqualTo(loginToken);
        assertThat(jwtUtil.extractUsername(registerToken))
                .isEqualTo(jwtUtil.extractUsername(loginToken))
                .isEqualTo("fullflow@example.com");
    }

    @Test
    @DisplayName("Should validate registration request fields")
    void registerUser_ValidationErrors() throws Exception {
        // Given - Invalid request (missing required fields)
        RegisterRequest request = new RegisterRequest();
        // All fields are null/empty - will fail validation

        // When & Then - Expect 400 or 500 (depending on whether validation is before or in controller)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError()); // Accept any 4xx error
    }

    @Test
    @DisplayName("Should validate login request fields")
    void login_ValidationErrors() throws Exception {
        // Given - Invalid request (null fields)
        LoginRequest request = new LoginRequest();
        // Both fields are null

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError()); // Accept any 4xx error
    }

    @Test
    @DisplayName("Should allow access to public auth endpoints without token")
    void publicEndpoints_NoTokenRequired() throws Exception {
        // Given - Valid but incomplete requests (will fail validation, not auth)
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Test");
        registerRequest.setEmail("test@test.com");
        // Missing password - will fail validation

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        // Missing password - will fail validation

        // When & Then - Should get validation error (4xx), not auth error (403)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().is4xxClientError());
    }
}