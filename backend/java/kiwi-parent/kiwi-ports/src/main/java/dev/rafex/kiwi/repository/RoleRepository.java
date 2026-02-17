package dev.rafex.kiwi.repository;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {

    Optional<RoleRow> findByName(String name) throws SQLException;

    void assignRoleToUser(UUID userId, UUID roleId) throws SQLException;

    public record RoleRow(UUID roleId, String name, String description, String status, Instant createdAt,
            Instant updatedAt) {
    }
}
