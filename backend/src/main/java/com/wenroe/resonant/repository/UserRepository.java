package com.wenroe.resonant.repository;

import com.wenroe.resonant.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by email.
     *
     * @param email the email to search for
     * @return Optional of User
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all users by role.
     *
     * @param role the role to filter by
     * @return List of Users with the specified role
     */
    List<User> findByRole(User.UserRole role);

    /**
     * Find all enabled users.
     *
     * @param enabled the enabled status
     * @return List of enabled/disabled users
     */
    List<User> findByEnabled(Boolean enabled);

    /**
     * Check if a user exists with the given email.
     *
     * @param email the email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);
}