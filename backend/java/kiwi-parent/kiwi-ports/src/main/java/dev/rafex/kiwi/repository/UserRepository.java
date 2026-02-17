package dev.rafex.kiwi.repository;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<UserRow> findByUsername(String username) throws SQLException;

    List<String> findRoleNamesByUserId(UUID userId) throws SQLException;

    Optional<UserWithRoles> findByUsernameWithRoles(String username) throws SQLException;

    public record UserRow(UUID userId, String username, byte[] passwordHash, byte[] salt, int iterations, String status,
            Instant createdAt, Instant updatedAt) {
    }

    public record UserWithRoles(UserRow user, List<String> roles) {
    }
}
