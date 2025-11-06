package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.UserRole;
import com.wenroe.resonant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for User operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get all users.
     *
     * @return List of all users
     */
    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Get user by ID.
     *
     * @param id the user ID
     * @return Optional of User
     */
    public Optional<User> getUserById(UUID id) {
        log.debug("Fetching user by id: {}", id);
        return userRepository.findById(id);
    }

    /**
     * Get user by email.
     *
     * @param email the email
     * @return Optional of User
     */
    public Optional<User> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Get users by role.
     *
     * @param role the role
     * @return List of users with the specified role
     */
    public List<User> getUsersByRole(UserRole role) {
        log.debug("Fetching users by role: {}", role);
        return userRepository.findByRole(role);
    }

    /**
     * Create a new user.
     *
     * @param user the user to create
     * @return the created user
     */
    @Transactional
    public User createUser(User user) {
        log.debug("Creating user: {}", user.getEmail());

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("User with email " + user.getEmail() + " already exists");
        }

        return userRepository.save(user);
    }

    /**
     * Update an existing user.
     *
     * @param id the user ID
     * @param updatedUser the updated user data
     * @return the updated user
     */
    @Transactional
    public User updateUser(UUID id, User updatedUser) {
        log.debug("Updating user: {}", id);

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        // Update fields
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setEnabled(updatedUser.getEnabled());

        if (updatedUser.getPasswordHash() != null) {
            existingUser.setPasswordHash(updatedUser.getPasswordHash());
        }

        return userRepository.save(existingUser);
    }

    /**
     * Delete a user.
     *
     * @param id the user ID
     */
    @Transactional
    public void deleteUser(UUID id) {
        log.debug("Deleting user: {}", id);

        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }

        userRepository.deleteById(id);
    }

    /**
     * Check if a user exists by email.
     *
     * @param email the email
     * @return true if exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}