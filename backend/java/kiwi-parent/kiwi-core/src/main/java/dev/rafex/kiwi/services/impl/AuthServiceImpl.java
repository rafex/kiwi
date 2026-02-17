package dev.rafex.kiwi.services.impl;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

import dev.rafex.kiwi.repository.UserRepository;
import dev.rafex.kiwi.security.PasswordHasherPBKDF2;
import dev.rafex.kiwi.services.AuthService;

public final class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final PasswordHasherPBKDF2 hasher;

    public AuthServiceImpl(final UserRepository userRepo, final PasswordHasherPBKDF2 hasher) {
        this.userRepo = Objects.requireNonNull(userRepo);
        this.hasher = Objects.requireNonNull(hasher);
    }

    @Override
    public AuthResult authenticate(final String username, final char[] password) throws Exception {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            return AuthResult.bad("bad_credentials");
        }

        try {
            final var userOpt = userRepo.findByUsername(username);
            if (userOpt.isEmpty()) {
                return AuthResult.bad("bad_credentials");
            }

            final var user = userOpt.get();

            // status check
            if (user.status() == null || !"active".equalsIgnoreCase(user.status())) {
                // aquí sí conviene distinguir, para que el frontend sepa que está bloqueado
                return AuthResult.bad("user_disabled");
            }

            // password verify
            final var ok = hasher.verify(password, user.salt(), user.iterations(), user.passwordHash());
            if (!ok) {
                return AuthResult.bad("bad_credentials");
            }

            // roles (activos)
            final var roles = userRepo.findRoleNamesByUserId(user.userId());

            return AuthResult.ok(user.userId(), user.username(), roles);

        } catch (final SQLException e) {
            // loguea arriba (handler) si quieres; aquí regresamos genérico
            return AuthResult.bad("error");
        } finally {
            // higiene: borra password
            Arrays.fill(password, '\0');
        }
    }
}