package dev.rafex.kiwi.server;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.sql.DataSource;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.kiwi.db.Db;
import dev.rafex.kiwi.db.LocationRepository;
import dev.rafex.kiwi.db.ObjectRepository;
import dev.rafex.kiwi.handlers.HelloHandler;
import dev.rafex.kiwi.handlers.LocationCreateHandler;
import dev.rafex.kiwi.handlers.ObjectCreateHandler;
import dev.rafex.kiwi.handlers.ObjectMoveHandler;
import dev.rafex.kiwi.handlers.ObjectSearchHandler;
import dev.rafex.kiwi.logging.Log;

public class RouterHandler extends Handler.Abstract {

    private final DataSource dataSource = Db.dataSource();
    private final HelloHandler helloHandler = new HelloHandler();
    private final LocationCreateHandler locationCreateHandler = new LocationCreateHandler(
            new LocationRepository(dataSource));

    private final ObjectRepository objectRepo = new ObjectRepository(dataSource);
    private final ObjectCreateHandler objectCreateHandler = new ObjectCreateHandler(objectRepo);
    private final ObjectMoveHandler objectMoveHandler = new ObjectMoveHandler(objectRepo);
    private final ObjectSearchHandler objectSearchHandler = new ObjectSearchHandler(objectRepo);

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) {
        try {
            final var method = request.getMethod();
            final var path = request.getHttpURI().getPath();

            if ("GET".equals(method) && "/hello".equals(path)) {
                return helloHandler.handle(request, response, callback);
            }

            if ("POST".equals(method) && "/objects".equals(path)) {
                Log.info(getClass(), "Handling object creation request");
                return objectCreateHandler.handle(request, response, callback);
            }

            if ("POST".equals(method) && "/locations".equals(path)) {
                return locationCreateHandler.handle(request, response, callback);
            }

            // POST /objects/{id}/move
            if ("POST".equals(method) && path.startsWith("/objects/") && path.endsWith("/move")) {
                // path = /objects/{uuid}/move
                final var parts = path.split("/");
                // ["", "objects", "{uuid}", "move"]
                if (parts.length == 4) {
                    try {
                        final var objectId = UUID.fromString(parts[2]);
                        return objectMoveHandler.handle(request, response, callback, objectId);
                    } catch (final IllegalArgumentException e) {
                        dev.rafex.kiwi.http.HttpUtil.badRequest(response, callback, "invalid UUID in path");
                        return true;
                    }
                }
            }

            if ("GET".equals(method) && "/objects/search".equals(path)) {
                return objectSearchHandler.handle(request, response, callback);
            }

            response.setStatus(404);
            response.getHeaders().put("content-type", "application/json; charset=utf-8");
            final var body = "{\"error\":\"not_found\"}".getBytes(StandardCharsets.UTF_8);
            response.write(true, java.nio.ByteBuffer.wrap(body), callback);
            callback.succeeded();
            return true;
        } catch (final Throwable t) {

            Log.error(getClass(), "Error handling request", t);

            response.setStatus(500);
            callback.failed(t);
            return true;
        }
    }
}
