package dev.rafex.kiwi.services;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface UserProvisioningService {

    CreateUserResult createUser(final String username, final char[] password, final List<String> roles)
            throws SQLException;

    boolean existsAnyUser() throws SQLException;

    public record CreateUserResult(boolean ok, UUID userId, String code) {
        public static CreateUserResult ok(final UUID userId) {
            return new CreateUserResult(true, userId, null);
        }

        public static CreateUserResult bad(final String code) {
            return new CreateUserResult(false, null, code);
        }
    }

}
