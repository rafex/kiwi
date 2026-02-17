package dev.rafex.kiwi.services;

import java.util.List;
import java.util.UUID;

public interface AuthService {

    AuthResult authenticate(String username, char[] password) throws Exception;

    public record AuthResult(boolean ok, UUID userId, String username, List<String> roles, String code) {
        public static AuthResult ok(final UUID userId, final String username, final List<String> roles) {
            return new AuthResult(true, userId, username, roles, null);
        }

        public static AuthResult bad(final String code) {
            return new AuthResult(false, null, null, List.of(), code);
        }
    }
}