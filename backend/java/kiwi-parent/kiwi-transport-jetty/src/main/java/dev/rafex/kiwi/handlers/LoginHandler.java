package dev.rafex.kiwi.handlers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.fasterxml.jackson.databind.JsonNode;

import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.json.JsonUtil;
import dev.rafex.kiwi.security.JwtService;

public final class LoginHandler extends Handler.Abstract.NonBlocking {

    private final JwtService jwt;
    private final long ttlSeconds;
    private final String expectedUser;
    private final String expectedPass;

    public LoginHandler(final JwtService jwt) {
        this(jwt, Long.parseLong(System.getenv().getOrDefault("JWT_TTL_SECONDS", "3600")),
                System.getenv().getOrDefault("KIWI_USER", "rafex"),
                System.getenv().getOrDefault("KIWI_PASS", UUID.randomUUID().toString()));
    }

    public LoginHandler(final JwtService jwt, final long ttlSeconds, final String expectedUser,
            final String expectedPass) {
        this.jwt = Objects.requireNonNull(jwt);
        this.ttlSeconds = ttlSeconds;
        this.expectedUser = Objects.requireNonNull(expectedUser);
        this.expectedPass = Objects.requireNonNull(expectedPass);
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            HttpUtil.json(response, callback, HttpStatus.METHOD_NOT_ALLOWED_405, Map.of("error", "method_not_allowed"));
            return true;
        }

        // 1) Intenta Basic Auth: Authorization: Basic base64(user:pass)
        final var authz = request.getHeaders().get("authorization");
        if (authz != null && authz.regionMatches(true, 0, "Basic ", 0, "Basic ".length())) {
            final var creds = decodeBasic(authz.substring("Basic ".length()).trim());
            if (creds == null) {
                HttpUtil.unauthorized(response, callback, "bad_basic_auth");
                return true;
            }

            if (!safeEquals(expectedUser, creds.user) || !safeEquals(expectedPass, creds.pass)) {
                HttpUtil.unauthorized(response, callback, "bad_credentials");
                return true;
            }

            return okToken(response, callback, creds.user);
        }

        // 2) Si no hay Basic, intenta body JSON: {"username":"...","password":"..."}
        // Nota: para “mínimo viable”, leemos el body como String. Si luego quieres 100%
        // async,
        // lo cambiamos a Content.Source + callback.
        final String body;
        try {
            body = Content.Source.asString(request, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            HttpUtil.badRequest(response, callback, "cannot_read_body");
            return true;
        }

        if (body == null || body.isBlank()) {
            HttpUtil.unauthorized(response, callback, "missing_credentials");
            return true;
        }

        final JsonNode json;
        try {
            json = JsonUtil.MAPPER.readTree(body);
        } catch (final Exception e) {
            HttpUtil.badRequest(response, callback, "invalid_json");
            return true;
        }

        final var user = text(json, "username");
        final var pass = text(json, "password");

        if (user == null || pass == null) {
            HttpUtil.unauthorized(response, callback, "missing_credentials");
            return true;
        }

        if (!safeEquals(expectedUser, user) || !safeEquals(expectedPass, pass)) {
            HttpUtil.unauthorized(response, callback, "bad_credentials");
            return true;
        }

        return okToken(response, callback, user);
    }

    private boolean okToken(final Response response, final Callback callback, final String user) {
        final var token = jwt.mint(user, ttlSeconds);

        HttpUtil.ok(response, callback,
                Map.of("token_type", "Bearer", "access_token", token, "expires_in", ttlSeconds));
        return true;
    }

    private static String text(final JsonNode node, final String field) {
        final var v = node.get(field);
        return v != null && v.isTextual() ? v.asText() : null;
    }

    private static boolean safeEquals(final String a, final String b) {
        // comparación en tiempo constante (minimiza timing leaks)
        final var ba = a.getBytes(StandardCharsets.UTF_8);
        final var bb = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(ba, bb);
    }

    private record BasicCreds(String user, String pass) {
    }

    private static BasicCreds decodeBasic(final String base64Part) {
        try {
            final var decoded = new String(Base64.getDecoder().decode(base64Part), StandardCharsets.UTF_8);
            final var idx = decoded.indexOf(':');
            if (idx <= 0) {
                return null;
            }
            final var user = decoded.substring(0, idx);
            final var pass = decoded.substring(idx + 1);
            return new BasicCreds(user, pass);
        } catch (final Exception e) {
            return null;
        }
    }
}
