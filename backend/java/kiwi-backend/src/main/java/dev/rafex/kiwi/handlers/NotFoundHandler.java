package dev.rafex.kiwi.handlers;

import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public final class NotFoundHandler extends Handler.Abstract {

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

        response.setStatus(HttpStatus.NOT_FOUND_404);
        response.getHeaders().put("content-type", "application/json; charset=utf-8");

        final var body = """
                {
                  "error": "Not Found",
                  "path": "%s"
                }
                """.formatted(request.getHttpURI().getPath());

        final var buffer = StandardCharsets.UTF_8.encode(body);

        response.write(true, buffer, callback);

        return true; // Siempre consume la request
    }
}