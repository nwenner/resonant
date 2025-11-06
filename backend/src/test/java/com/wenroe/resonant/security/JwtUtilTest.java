package com.wenroe.resonant.security;

import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;
    private String testSecret;
    private Long testExpiration;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        // Set test values using reflection (since these are @Value injected)
        testSecret = "test-jwt-secret-key-that-is-long-enough-for-hmac-sha-256-algorithm-minimum-256-bits";
        testExpiration = 3600000L; // 1 hour

        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", testExpiration);

        // Setup test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void generateToken_Success() {
        // When
        String token = jwtUtil.generateToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("Should extract username from token")
    void extractUsername_Success() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        String username = jwtUtil.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void extractExpiration_Success() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        Date expiration = jwtUtil.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date()); // Should be in the future

        // Should expire roughly 1 hour from now (with 1 minute tolerance)
        long expectedExpiration = System.currentTimeMillis() + testExpiration;
        long actualExpiration = expiration.getTime();
        assertThat(Math.abs(expectedExpiration - actualExpiration)).isLessThan(60000); // Within 1 minute
    }

    @Test
    @DisplayName("Should validate token with correct user")
    void validateToken_Valid() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        Boolean isValid = jwtUtil.validateToken(token, testUser);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject token with wrong user")
    void validateToken_WrongUser() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setName("Other User");

        // When
        Boolean isValid = jwtUtil.validateToken(token, otherUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject expired token")
    void validateToken_Expired() {
        // Given - Create a token that expires immediately
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1L); // 1 millisecond
        String token = jwtUtil.generateToken(testUser);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Reset expiration for validation
        ReflectionTestUtils.setField(jwtUtil, "expiration", testExpiration);

        // When & Then
        assertThatThrownBy(() -> jwtUtil.validateToken(token, testUser))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should reject token with tampered signature")
    void validateToken_TamperedSignature() {
        // Given
        String validToken = jwtUtil.generateToken(testUser);

        // Tamper with the signature (last part of token)
        String[] parts = validToken.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".tampered-signature";

        // When & Then
        assertThatThrownBy(() -> jwtUtil.extractUsername(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should reject token signed with different secret")
    void validateToken_WrongSecret() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // Create another JwtUtil with different secret
        JwtUtil otherJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherJwtUtil, "secret", "different-secret-key-that-is-also-long-enough-for-hmac-256");
        ReflectionTestUtils.setField(otherJwtUtil, "expiration", testExpiration);

        // When & Then - Should fail to parse with different secret
        assertThatThrownBy(() -> otherJwtUtil.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void generateToken_DifferentUsers() {
        // Given
        User user1 = new User();
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.setEmail("user2@example.com");

        // When
        String token1 = jwtUtil.generateToken(user1);
        String token2 = jwtUtil.generateToken(user2);

        // Then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtUtil.extractUsername(token1)).isEqualTo("user1@example.com");
        assertThat(jwtUtil.extractUsername(token2)).isEqualTo("user2@example.com");
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void generateToken_DifferentTimes() throws InterruptedException {
        // Given & When
        String token1 = jwtUtil.generateToken(testUser);
        Thread.sleep(1100); // Wait over 1 second to ensure different timestamp (JWT uses seconds)
        String token2 = jwtUtil.generateToken(testUser);

        // Then
        assertThat(token1).isNotEqualTo(token2); // Different because of different issuedAt time
        assertThat(jwtUtil.extractUsername(token1)).isEqualTo(jwtUtil.extractUsername(token2));
    }

    @Test
    @DisplayName("Should use email as subject in token")
    void generateToken_EmailAsSubject() {
        // Given
        testUser.setEmail("specific@example.com");

        // When
        String token = jwtUtil.generateToken(testUser);
        String extractedEmail = jwtUtil.extractUsername(token);

        // Then
        assertThat(extractedEmail).isEqualTo("specific@example.com");
    }

    @Test
    @DisplayName("Should reject null token")
    void validateToken_NullToken() {
        // When & Then
        assertThatThrownBy(() -> jwtUtil.extractUsername(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject empty token")
    void validateToken_EmptyToken() {
        // When & Then
        assertThatThrownBy(() -> jwtUtil.extractUsername(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject malformed token")
    void validateToken_MalformedToken() {
        // When & Then
        assertThatThrownBy(() -> jwtUtil.extractUsername("not.a.valid.jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token should not be expired immediately after creation")
    void generateToken_NotExpiredImmediately() {
        // Given
        String token = jwtUtil.generateToken(testUser);

        // When
        Date expiration = jwtUtil.extractExpiration(token);

        // Then
        assertThat(expiration).isAfter(new Date());
        assertThat(jwtUtil.validateToken(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("Should handle special characters in email")
    void generateToken_SpecialCharactersInEmail() {
        // Given
        testUser.setEmail("test+filter@example.co.uk");

        // When
        String token = jwtUtil.generateToken(testUser);
        String extractedEmail = jwtUtil.extractUsername(token);

        // Then
        assertThat(extractedEmail).isEqualTo("test+filter@example.co.uk");
    }
}