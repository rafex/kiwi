package dev.rafex.kiwi.repository.impl;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import dev.rafex.kiwi.repository.RoleRepository;

public class RoleRepositoryImpl implements RoleRepository {

    private final DataSource ds;

    public RoleRepositoryImpl(final DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Optional<RoleRow> findByName(final String name) throws SQLException {
        final var sql = """
                SELECT role_id, name, description, status, created_at, updated_at
                FROM roles
                WHERE name = ?
                """;

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {

            ps.setString(1, name);

            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(new RoleRow(rs.getObject("role_id", UUID.class), rs.getString("name"),
                        rs.getString("description"), rs.getString("status"), rs.getObject("created_at", Instant.class),
                        rs.getObject("updated_at", Instant.class)));
            }
        }
    }

    @Override
    public void assignRoleToUser(final UUID userId, final UUID roleId) throws SQLException {
        final var sql = """
                INSERT INTO user_roles(user_id, role_id)
                VALUES (?, ?)
                ON CONFLICT (user_id, role_id) DO NOTHING
                """;

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {

            ps.setObject(1, userId);
            ps.setObject(2, roleId);
            ps.executeUpdate();
        }
    }

}
