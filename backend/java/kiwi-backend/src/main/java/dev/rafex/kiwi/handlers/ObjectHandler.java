package dev.rafex.kiwi.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.sql.DataSource;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.rafex.kiwi.db.Db;
import dev.rafex.kiwi.db.ObjectRepository;
import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.handlers.dto.CreateObjectRequest;
import dev.rafex.kiwi.handlers.dto.MoveObjectRequest;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.services.ObjectServices;

public class ObjectHandler extends Handler.Abstract {

    private final DataSource dataSource = Db.dataSource();
    private final ObjectRepository objectRepo = new ObjectRepository(dataSource);
    private final ObjectMapper om = new ObjectMapper();

    private final ObjectServices services = new ObjectServices(objectRepo);

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) {
        try {
            final var method = request.getMethod();
            final var path = request.getHttpURI().getPath();

            if ("POST".equals(method) && "/objects".equals(path)) {
                Log.info(getClass(), "Handling object creation request");
                return create(request, response, callback);
            }

            // POST /objects/{id}/move
            if ("POST".equals(method) && path.startsWith("/objects/") && path.endsWith("/move")) {
                // path = /objects/{uuid}/move
                final var parts = path.split("/");
                // ["", "objects", "{uuid}", "move"]
                if (parts.length == 4) {
                    try {
                        final var objectId = UUID.fromString(parts[2]);
                        return move(request, response, callback, objectId);
                    } catch (final IllegalArgumentException e) {
                        HttpUtil.badRequest(response, callback, "invalid UUID in path");
                        return true;
                    }
                }
            }

            if ("GET".equals(method) && "/objects/search".equals(path)) {
                return services.search(request, response, callback);
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

    private boolean move(final Request request, final Response response, final Callback callback, final UUID objectId) {

        try {
            Log.info(getClass(), "Handling object move request");

            final var bytes = Request.asInputStream(request).readAllBytes();
            final var body = new String(bytes, StandardCharsets.UTF_8);
            final var r = om.readValue(body, MoveObjectRequest.class);

            if (r.getNewLocationId() == null || r.getNewLocationId().isBlank()) {
                HttpUtil.badRequest(response, callback, "newLocationId is required");
                return true;
            }

            UUID newLocationId;
            try {
                newLocationId = UUID.fromString(r.getNewLocationId());
            } catch (final IllegalArgumentException e) {
                HttpUtil.badRequest(response, callback, "invalid UUID in newLocationId");
                return true;
            }

            services.move(objectId, newLocationId);

            // 204 No Content
            response.setStatus(204);
            callback.succeeded();
            return true;

        } catch (final KiwiError e) {

            return switch (e.getCode()) {
            case "E-001" -> {
                HttpUtil.badRequest(response, callback, "newLocationId does not exist");
                yield true;
            }
            case "E-002" -> {
                HttpUtil.badRequest(response, callback, "invalid new location");
                yield true;
            }
            default -> {
                Log.error(getClass(), "KiwiError moving object", e);
                HttpUtil.json(response, callback, 400, "{\"error\":\"" + e.getCode() + "\"}");
                yield true;
            }
            };

        } catch (final IOException e1) {
            Log.error(getClass(), "KiwiError moving object", e1);
            HttpUtil.json(response, callback, 400, "{\"error\":\"" + e1.getMessage() + "\"}");
            return true;
        }
    }

    private boolean create(final Request request, final Response response, final Callback callback) {
        try {
            final var bytes = Request.asInputStream(request).readAllBytes();
            final var body = new String(bytes, StandardCharsets.UTF_8);

            final var r = om.readValue(body, CreateObjectRequest.class);

            // validaciones mínimas
            if (r.getName() == null || r.getName().isBlank()) {
                HttpUtil.badRequest(response, callback, "name is required");
                return true;
            }
            if (r.getLocationId() == null || r.getLocationId().isBlank()) {
                HttpUtil.badRequest(response, callback, "locationId is required");
                return true;
            }

            final var objectId = UUID.randomUUID();
            final var locationId = UUID.fromString(r.getLocationId());

            final var tags = r.getTags() == null ? null : r.getTags().toArray(new String[0]);
            final var metadataJson = r.getMetadata() == null ? null : om.writeValueAsString(r.getMetadata());

            services.create(objectId, r.getName(), r.getDescription(), r.getType(), tags, metadataJson, locationId);

            HttpUtil.json(response, callback, 201, "{\"object_id\":\"" + objectId + "\"}");
            return true;

        } catch (final IllegalArgumentException e) {
            Log.error(getClass(), "Invalid UUID format", e);
            HttpUtil.badRequest(response, callback, "invalid UUID");
            return true;
        } catch (final Exception e) {
            Log.error(getClass(), "Error creating object", e);
            HttpUtil.json(response, callback, 500, "{\"error\":\"internal_error\"}");
            return true;
        }
    }
}
