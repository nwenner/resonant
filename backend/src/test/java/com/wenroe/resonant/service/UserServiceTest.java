package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .name("Test User")
                .passwordHash("hashed_password")
                .role(User.UserRole.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should return all users")
    void getAllUsers_Success() {
        // Given
        User user2 = User.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .name("Admin User")
                .passwordHash("hashed_password")
                .role(User.UserRole.ADMIN)
                .enabled(true)
                .build();

        List<User> users = List.of(testUser, user2);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.getAllUsers();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(testUser, user2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should return user by ID when user exists")
    void getUserById_WhenExists() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserById(testUserId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        verify(userRepository).findById(testUserId);
    }

    @Test
    @DisplayName("Should return empty when user does not exist")
    void getUserById_WhenNotExists() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserById(nonExistentId);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should return user by email when user exists")
    void getUserByEmail_WhenExists() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserByEmail(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(email);
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Should return users by role")
    void getUsersByRole_Success() {
        // Given
        List<User> users = List.of(testUser);
        when(userRepository.findByRole(User.UserRole.USER)).thenReturn(users);

        // When
        List<User> result = userService.getUsersByRole(User.UserRole.USER);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(User.UserRole.USER);
        verify(userRepository).findByRole(User.UserRole.USER);
    }

    @Test
    @DisplayName("Should create user with valid data")
    void createUser_Success() {
        // Given
        User newUser = User.builder()
                .email("new@example.com")
                .name("New User")
                .passwordHash("hashed_password")
                .role(User.UserRole.USER)
                .enabled(true)
                .build();

        when(userRepository.existsByEmail(newUser.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        User result = userService.createUser(newUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(userRepository).existsByEmail(newUser.getEmail());
        verify(userRepository).save(newUser);
    }

    @Test
    @DisplayName("Should throw exception when creating user with duplicate email")
    void createUser_DuplicateEmail() {
        // Given
        User newUser = User.builder()
                .email("test@example.com")
                .name("Test User")
                .passwordHash("hashed_password")
                .role(User.UserRole.USER)
                .enabled(true)
                .build();

        when(userRepository.existsByEmail(newUser.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(newUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(userRepository).existsByEmail(newUser.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user when user exists")
    void updateUser_Success() {
        // Given
        User updatedData = User.builder()
                .email("updated@example.com")
                .name("Updated User")
                .passwordHash("new_hashed_password")
                .role(User.UserRole.ADMIN)
                .enabled(false)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUser(testUserId, updatedData);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(testUserId);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void updateUser_UserNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        User updatedData = User.builder()
                .email("updated@example.com")
                .name("Updated User")
                .passwordHash("hashed_password")
                .role(User.UserRole.USER)
                .enabled(true)
                .build();

        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(nonExistentId, updatedData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");

        verify(userRepository).findById(nonExistentId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should delete user when user exists")
    void deleteUser_Success() {
        // Given
        when(userRepository.existsById(testUserId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(testUserId);

        // When
        userService.deleteUser(testUserId);

        // Then
        verify(userRepository).existsById(testUserId);
        verify(userRepository).deleteById(testUserId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void deleteUser_UserNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.existsById(nonExistentId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");

        verify(userRepository).existsById(nonExistentId);
        verify(userRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should return true when user exists by email")
    void existsByEmail_WhenExists() {
        // Given
        String email = "test@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When
        boolean result = userService.existsByEmail(email);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("Should return false when user does not exist by email")
    void existsByEmail_WhenNotExists() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);

        // When
        boolean result = userService.existsByEmail(email);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail(email);
    }
}