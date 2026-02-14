package dev.rafex.kiwi.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.kiwi.json.JsonUtil;

public final class HttpUtil {

    private HttpUtil() {
    }

    /**
     * Escribe una respuesta JSON. Si `body` es una cadena se asume JSON ya formateado,
     * en caso contrario se serializa con `JsonUtil.MAPPER`.
     */
    public static void json(final Response response, final Callback callback, final int status, final Object body) {
        response.setStatus(status);
        response.getHeaders().put("content-type", "application/json; charset=utf-8");

        final String jsonBody = body instanceof String ? (String) body : JsonUtil.toJson(body);
        final var bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        response.write(true, ByteBuffer.wrap(bytes), callback);
    }

    public static void json(final Response response, final Callback callback, final int status, final String jsonBody) {
        json(response, callback, status, (Object) jsonBody);
    }

    public static void ok(final Response response, final Callback callback, final Object body) {
        json(response, callback, 200, body);
    }

    public static void ok(final Response response, final Callback callback, final String jsonBody) {
        json(response, callback, 200, (Object) jsonBody);
    }

    public static void notFound(final Response response, final Callback callback) {
        json(response, callback, 404, Map.of("error", "not_found"));
    }

    public static void badRequest(final Response response, final Callback callback, final String message) {
        json(response, callback, 400, Map.of("error", "bad_request", "message", message));
    }

}
