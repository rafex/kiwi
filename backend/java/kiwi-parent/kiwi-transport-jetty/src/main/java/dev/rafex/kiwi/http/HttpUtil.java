package dev.rafex.kiwi.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.kiwi.json.JsonUtil;

public final class HttpUtil {

    private static final Map<String, String> NOT_FOUND_BODY = Map.of("error", "not_found");

    private HttpUtil() {
    }

    /**
     * Escribe una respuesta JSON. Si `body` es una cadena se asume JSON ya
     * formateado, en caso contrario se serializa con `JsonUtil.MAPPER`.
     */
    public static void json(final Response response, final Callback callback, final int status, final Object body) {
        response.setStatus(status);
        response.getHeaders().put("content-type", "application/json; charset=utf-8");

        final var jsonBody = body instanceof final String s ? s : JsonUtil.toJson(body);
        final var bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        response.write(true, ByteBuffer.wrap(bytes), callback);
    }

    public static void json(final Response response, final Callback callback, final int status, final String jsonBody) {
        json(response, callback, status, (Object) jsonBody);
    }

    public static void ok(final Response response, final Callback callback, final Object body) {
        json(response, callback, HttpStatus.OK_200, body);
    }

    public static void ok(final Response response, final Callback callback, final String jsonBody) {
        json(response, callback, HttpStatus.OK_200, (Object) jsonBody);
    }

    public static void ok_noContent(final Response response, final Callback callback) {
        response.setStatus(HttpStatus.NO_CONTENT_204);
        callback.succeeded();
    }

    public static void notFound(final Response response, final Callback callback) {
        json(response, callback, HttpStatus.NOT_FOUND_404, NOT_FOUND_BODY);

    }

    public static void notFound(final Response response, final Callback callback, final String path) {
        json(response, callback, HttpStatus.NOT_FOUND_404,
                Map.of("error", "not_found", "path", path, "timestamp", Instant.now().toString()));

    }

    public static void badRequest(final Response response, final Callback callback, final String message) {
        json(response, callback, HttpStatus.BAD_REQUEST_400,
                Map.of("error", "bad_request", "message", message, "timestamp", Instant.now().toString()));
    }

    public static void internalServerError(final Response response, final Callback callback, final String message) {
        json(response, callback, HttpStatus.INTERNAL_SERVER_ERROR_500,
                Map.of("error", "internal_server_error", "message", message, "timestamp", Instant.now().toString()));
    }

    public static void unauthorized(final Response response, final Callback callback, final String code) {
        json(response, callback, HttpStatus.UNAUTHORIZED_401,
                Map.of("error", "unauthorized", "code", code, "timestamp", Instant.now().toString()));

    }

}
