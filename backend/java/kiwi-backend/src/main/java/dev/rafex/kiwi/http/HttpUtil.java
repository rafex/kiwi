package dev.rafex.kiwi.http;

import java.nio.ByteBuffer;
import java.util.Map;

import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.kiwi.json.JsonUtil;

public final class HttpUtil {

    private HttpUtil() {
    }

    public static void json(final Response response, final Callback callback, final int status, final Object body) {
        response.setStatus(status);
        response.getHeaders().put("content-type", "application/json; charset=utf-8");

        try {
            final byte[] bytes = JsonUtil.MAPPER.writeValueAsBytes(body);
            response.write(true, ByteBuffer.wrap(bytes), callback);
        } catch (final Exception e) {
            callback.failed(e);
        }
    }

    public static void ok(final Response response, final Callback callback, final Object body) {
        json(response, callback, 200, body);
    }

    public static void notFound(final Response response, final Callback callback) {
        json(response, callback, 404, Map.of("error", "not_found"));
    }

    public static void badRequest(final Response response, final Callback callback, final String message) {
        json(response, callback, 400, Map.of("error", "bad_request", "message", message));
    }
}
