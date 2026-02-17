package dev.rafex.kiwi.repository.impl;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import dev.rafex.kiwi.repository.UserRepository;

public class UserRepositoryImpl implements UserRepository {

    private final DataSource ds;

    public UserRepositoryImpl(final DataSource ds) {
        this.ds = ds;
    }

    @Override
    public void createUser(final UUID userId, final String username, final byte[] passwordHash, final byte[] salt,
            final int iterations) throws SQLException {

        final var sql = """
                INSERT INTO users (user_id,username, password_hash, salt, iterations, status, created_at, updated_at)
                VALUES (?,?, ?, ?, ?, 'active', NOW(), NOW())
                """;

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {

            ps.setObject(1, userId);
            ps.setString(2, username);
            ps.setBytes(3, passwordHash);
            ps.setBytes(4, salt);
            ps.setInt(5, iterations);

            ps.executeUpdate();
        }

    }

    /** Para login: trae hash/salt/iterations y status */
    @Override
    public Optional<UserRow> findByUsername(final String username) throws SQLException {
        final var sql = """
                SELECT user_id, username, password_hash, salt, iterations, status, created_at, updated_at
                FROM users
                WHERE username = ?
                """;

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {

            ps.setString(1, username);

            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(new UserRow(rs.getObject("user_id", UUID.class), rs.getString("username"),
                        rs.getBytes("password_hash"), rs.getBytes("salt"), rs.getInt("iterations"),
                        rs.getString("status"), rs.getObject("created_at", Instant.class),
                        rs.getObject("updated_at", Instant.class)));
            }
        }
    }

    /** Roles del usuario (para meterlos en JWT o checar permisos) */
    @Override
    public List<String> findRoleNamesByUserId(final UUID userId) throws SQLException {
        final var sql = """
                SELECT r.name
                FROM roles r
                JOIN user_roles ur ON ur.role_id = r.role_id
                WHERE ur.user_id = ?
                  AND r.status = 'active'
                ORDER BY r.name
                """;

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {

            ps.setObject(1, userId);

            try (var rs = ps.executeQuery()) {
                final var out = new ArrayList<String>();
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
                return out;
            }
        }
    }

    /** Útil si quieres obtener todo (usuario + roles) en una sola llamada */
    @Override
    public Optional<UserWithRoles> findByUsernameWithRoles(final String username) throws SQLException {
        final var userOpt = findByUsername(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        final var user = userOpt.get();
        final var roles = findRoleNamesByUserId(user.userId());
        return Optional.of(new UserWithRoles(user, roles));
    }

}
