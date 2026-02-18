package dev.rafex.kiwi.handlers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import dev.rafex.kiwi.services.UserProvisioningService;

public final class CreateUserHandler extends Handler.Abstract.NonBlocking {

    private static final boolean PROVISIONING_ENABLED = "true"
            .equalsIgnoreCase(System.getenv().getOrDefault("ENABLE_USER_PROVISIONING", "false"));

    private static final String ENV = System.getenv().getOrDefault("ENVIRONMENT", "unknown"); // ej: work02 | live02

    private static final String BOOTSTRAP_TOKEN = System.getenv().getOrDefault("BOOTSTRAP_TOKEN", "");

    private final UserProvisioningService provisioning;

    public CreateUserHandler(final UserProvisioningService provisioning) {
        this.provisioning = Objects.requireNonNull(provisioning);
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

        if (!PROVISIONING_ENABLED || !isSandbox()) {
            HttpUtil.notFound(response, callback, request.getHttpURI().getPath());
            return true;
        }

        final var bootstrap = hasValidBootstrapToken(request);

        // Si no hay users -> solo permitimos BOOTSTRAP (para crear el primero)
        // Si ya hay users -> solo permitimos JWT admin
        final var existsAnyUser = provisioning.existsAnyUser();

        if (!existsAnyUser) {
            // primer usuario: debe venir bootstrap token
            if (!bootstrap) {
                HttpUtil.notFound(response, callback, request.getHttpURI().getPath());
                return true;
            }
            // si bootstrap ok, NO pedimos JWT (todavía no existe)
        } else {
            // ya hay usuarios: bootstrap ya NO debe servir
            if (bootstrap) {
                HttpUtil.notFound(response, callback, request.getHttpURI().getPath());
                return true;
            }

            // exige JWT admin
            final var authObj = request.getAttribute(JwtAuthHandler.REQ_ATTR_AUTH);
            if (authObj == null) {
                // para ocultar endpoint
                HttpUtil.notFound(response, callback, request.getHttpURI().getPath());
                return true;
            }

            if (authObj instanceof final JwtService.AuthContext ctx && !ctx.roles().contains("admin")) {
                HttpUtil.forbidden(response, callback, "missing_admin_role");
                return true;
            }
        }

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            HttpUtil.json(response, callback, HttpStatus.METHOD_NOT_ALLOWED_405, Map.of("error", "method_not_allowed"));
            return true;
        }

        // ---- Body ----
        final String body;
        try {
            body = Content.Source.asString(request, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            HttpUtil.badRequest(response, callback, "cannot_read_body");
            return true;
        }

        if (body == null || body.isBlank()) {
            HttpUtil.badRequest(response, callback, "missing_body");
            return true;
        }

        final JsonNode json;
        try {
            json = JsonUtil.MAPPER.readTree(body);
        } catch (final Exception e) {
            HttpUtil.badRequest(response, callback, "invalid_json");
            return true;
        }

        final var username = text(json, "username");
        final var password = text(json, "password");
        final var roles = roles(json.get("roles"));

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            HttpUtil.badRequest(response, callback, "missing_fields");
            return true;
        }

        final var res = provisioning.createUser(username, password.toCharArray(), roles);

        if (!res.ok()) {
            final var code = res.code();
            if ("username_taken".equals(code)) {
                HttpUtil.json(response, callback, HttpStatus.CONFLICT_409,
                        Map.of("error", "conflict", "code", "username_taken"));
            } else if ("invalid_input".equals(code)) {
                HttpUtil.badRequest(response, callback, "invalid_input");
            } else {
                HttpUtil.json(response, callback, HttpStatus.INTERNAL_SERVER_ERROR_500,
                        Map.of("error", "server_error", "code", code));
            }
            return true;
        }

        HttpUtil.ok(response, callback,
                Map.of("user_id", res.userId().toString(), "username", username, "roles", roles));
        return true;
    }

    private static String text(final JsonNode node, final String field) {
        if (node == null) {
            return null;
        }
        final var v = node.get(field);
        return v != null && v.isTextual() ? v.asText() : null;
    }

    private static List<String> roles(final JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }

        if (node.isTextual()) {
            // permite "roles":"admin"
            final var s = node.asText();
            return s == null || s.isBlank() ? List.of() : List.of(s);
        }

        if (!node.isArray()) {
            return List.of();
        }

        final var out = new ArrayList<String>();
        for (final var it : node) {
            if (it != null && it.isTextual()) {
                final var s = it.asText();
                if (s != null && !s.isBlank()) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    private static boolean hasValidBootstrapToken(final Request request) {
        final var t = request.getHeaders().get("x-bootstrap-token");
        return t != null && !BOOTSTRAP_TOKEN.isBlank() && MessageDigest.isEqual(t.getBytes(StandardCharsets.UTF_8),
                BOOTSTRAP_TOKEN.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isSandbox() {
        // ajusta a tus nombres reales
        return "work02".equalsIgnoreCase(ENV) || "sandbox".equalsIgnoreCase(ENV) || "dev".equalsIgnoreCase(ENV);
    }
}