package dev.rafex.kiwi.services;

public interface AuthService {
    AuthResult authenticate(String username, char[] password) throws Exception;

    public record AuthResult(boolean ok, String userId, // o UUID
            String username, String[] roles // opcional
    ) {
        public static AuthResult ok(final String userId, final String username, final String[] roles) {
            return new AuthResult(true, userId, username, roles);
        }

        public static AuthResult bad() {
            return new AuthResult(false, null, null, null);
        }
    }

}