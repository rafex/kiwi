package dev.rafex.kiwi.repository.impl;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import dev.rafex.kiwi.logging.Log;
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
    public UUID ensureRole(final String name, final String description) throws SQLException {
        // 1) intenta encontrarlo
        final var sel = "SELECT role_id FROM roles WHERE name = ?";
        try (var c = ds.getConnection(); var ps = c.prepareStatement(sel)) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1, UUID.class);
                }
            }
        } catch (final SQLException e) {
            Log.error(getClass(), "Error al buscar rol por nombre", e);
        }

        // 2) si no existe, créalo
        final var roleId = UUID.randomUUID();
        final var ins = """
                INSERT INTO roles (role_id, name, description, status)
                VALUES (?, ?, ?, 'active')
                """;
        try (var c = ds.getConnection(); var ps = c.prepareStatement(ins)) {
            ps.setObject(1, roleId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.executeUpdate();
        } catch (final SQLException e) {
            Log.error(getClass(), e, "Error al crear rol {}:{}", name, description);
            // Puede ser un error de concurrencia (otro proceso creó el mismo rol al mismo
            // tiempo)
            // Intenta encontrarlo de nuevo
            try (var c = ds.getConnection(); var ps = c.prepareStatement(sel)) {
                ps.setString(1, name);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getObject(1, UUID.class);
                    }
                }
            } catch (final SQLException ex) {
                Log.error(getClass(), ex, "Error al buscar rol por nombre después de fallo de inserción {}:{}", name,
                        description);
            }
        }

        return roleId;
    }

    @Override
    public void assignRoleToUser(final UUID userId, final UUID roleId) throws SQLException {
        final var sql = """
                SELECT api_assign_role_to_user(?::uuid, ?::uuid)
                """;

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {

            ps.setObject(1, userId);
            ps.setObject(2, roleId);
            ps.execute();
        }
    }

}
