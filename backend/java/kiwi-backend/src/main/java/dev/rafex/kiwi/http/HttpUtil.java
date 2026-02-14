package dev.rafex.kiwi.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.kiwi.json.JsonUtil;

public final class HttpUtil {

    private HttpUtil() {
    }

    public static void json(final Response response, final Callback callback, final int status, final String jsonBody) {
        response.setStatus(status);
        response.getHeaders().put("content-type", "application/json; charset=utf-8");

        final var bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        response.write(true, ByteBuffer.wrap(bytes), callback);
    }

    public static void ok(final Response response, final Callback callback, final String jsonBody) {
        json(response, callback, 200, jsonBody);
    }

    public static void notFound(final Response response, final Callback callback) {
        json(response, callback, 404, "{\"error\":\"not_found\"}");
    }

    public static void badRequest(final Response response, final Callback callback, final String message) {
        json(response, callback, 400, "{\"error\":\"bad_request\",\"message\":\"" + JsonUtil.escapeJson(message) + "\"}");
    }

}
