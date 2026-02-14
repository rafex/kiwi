package dev.rafex.kiwi.handlers;

import java.io.IOException;
import java.net.URLDecoder;
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
import dev.rafex.kiwi.dtos.CreateObjectRequest;
import dev.rafex.kiwi.dtos.MoveObjectRequest;
import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.json.JsonUtil;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.services.ObjectServices;

public class ObjectHandler extends Handler.Abstract {

    private final DataSource dataSource = Db.dataSource();
    private final ObjectRepository objectRepo = new ObjectRepository(dataSource);
    private final ObjectMapper om = JsonUtil.MAPPER;

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
                // /objects/{uuid}/move -> uuid starts at 9, ends before last 5 chars
                final var uuidStart = 9; // "/objects/".length()
                final var uuidEnd = path.length() - 5; // "/move".length()
                if (uuidEnd > uuidStart) {
                    try {
                        final var objectId = UUID.fromString(path.substring(uuidStart, uuidEnd));
                        return moveLocation(request, response, callback, objectId);
                    } catch (final IllegalArgumentException e) {
                        HttpUtil.badRequest(response, callback, "invalid UUID in path");
                        return true;
                    }
                }
            }

            if ("GET".equals(method) && "/objects/search".equals(path)) {
                return search(request, response, callback);
            }

            HttpUtil.notFound(response, callback);
            return true;
        } catch (final Throwable t) {

            Log.error(getClass(), "Error handling request", t);

            response.setStatus(500);
            callback.failed(t);
            return true;
        }
    }

    private boolean search(final Request request, final Response response, final Callback callback) {
        try {
            final var qp = request.getHttpURI().getQuery(); // raw query string
            final var q = queryParam(qp, "q");
            if (q == null || q.isBlank()) {
                HttpUtil.badRequest(response, callback, "q is required");
                return true;
            }

            final var tagsParam = queryParam(qp, "tags"); // "a,b,c"
            final var locationParam = queryParam(qp, "locationId"); // uuid
            final var limitParam = queryParam(qp, "limit"); // int

            final var tags = parseTags(tagsParam);
            final var locationId = parseUuidOrNull(locationParam);
            final var limit = parseLimit(limitParam, 20, 1, 200);

            final var out = om.createObjectNode();
            final var search = services.search(q.trim(), tags, locationId, limit);
            out.set("items", om.valueToTree(search));

            HttpUtil.ok(response, callback, om.writeValueAsString(out));
            return true;

        } catch (final IllegalArgumentException e) {
            HttpUtil.badRequest(response, callback, e.getMessage());
            return true;

        } catch (final Exception e) {
            Log.error(getClass(), "Error searching objects", e);
            HttpUtil.json(response, callback, 500, "{\"error\":\"internal_error\"}");
            return true;
        }
    }

    // -------- helpers (sin libs extra) --------

    private static String queryParam(final String rawQuery, final String key) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return null;
        }

        // rawQuery example: "q=laptop&tags=a,b&limit=20"
        for (final String pair : rawQuery.split("&")) {
            final var idx = pair.indexOf('=');
            final var k = idx >= 0 ? pair.substring(0, idx) : pair;
            if (!k.equals(key)) {
                continue;
            }

            final var v = idx >= 0 ? pair.substring(idx + 1) : "";
            return URLDecoder.decode(v, StandardCharsets.UTF_8);
        }
        return null;
    }

    private static String[] parseTags(final String tagsParam) {
        if (tagsParam == null) {
            return null;
        }
        final var t = tagsParam.trim();
        if (t.isEmpty()) {
            return null;
        }

        // soporta "a,b,c" o "a"
        final var parts = t.split(",");
        // limpia espacios y descarta vacíos
        var count = 0;
        for (final String p : parts) {
            if (!p.trim().isEmpty()) {
                count++;
            }
        }
        if (count == 0) {
            return null;
        }

        final var out = new String[count];
        var i = 0;
        for (final String p : parts) {
            final var s = p.trim();
            if (!s.isEmpty()) {
                out[i] = s;
                i++;
            }
        }
        return out;
    }

    private static UUID parseUuidOrNull(final String uuidStr) {
        if (uuidStr == null) {
            return null;
        }
        final var v = uuidStr.trim();
        if (v.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(v);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid UUID in locationId");
        }
    }

    private static int parseLimit(final String limitStr, final int def, final int min, final int max) {
        if (limitStr == null || limitStr.trim().isEmpty()) {
            return def;
        }
        try {
            final var v = Integer.parseInt(limitStr.trim());
            if (v < min) {
                return min;
            }
            if (v > max) {
                return max;
            }
            return v;
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("limit must be an integer");
        }
    }

    private boolean moveLocation(final Request request, final Response response, final Callback callback, final UUID objectId) {

        try {
            Log.info(getClass(), "Handling object move request");

            final var r = om.readValue(Request.asInputStream(request), MoveObjectRequest.class);

            if (r.newLocationId() == null || r.newLocationId().isBlank()) {
                HttpUtil.badRequest(response, callback, "newLocationId is required");
                return true;
            }

            UUID newLocationId;
            try {
                newLocationId = UUID.fromString(r.newLocationId());
            } catch (final IllegalArgumentException e) {
                HttpUtil.badRequest(response, callback, "invalid UUID in newLocationId");
                return true;
            }

            services.move(objectId, newLocationId);

            // 204 No Content
            HttpUtil.ok_noContent(response, callback);
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

            final var r = om.readValue(Request.asInputStream(request), CreateObjectRequest.class);

            // validaciones mínimas
            if (r.name() == null || r.name().isBlank()) {
                HttpUtil.badRequest(response, callback, "name is required");
                return true;
            }
            if (r.locationId() == null || r.locationId().isBlank()) {
                HttpUtil.badRequest(response, callback, "locationId is required");
                return true;
            }

            final var objectId = UUID.randomUUID();
            final var locationId = UUID.fromString(r.locationId());

            final var tags = r.tags() == null ? null : r.tags().toArray(new String[0]);
            final var metadataJson = r.metadata() == null ? null : om.writeValueAsString(r.metadata());

            services.create(objectId, r.name(), r.description(), r.type(), tags, metadataJson, locationId);

            HttpUtil.json(response, callback, 201, "{\"object_id\":\"" + objectId + "\"}");
            return true;

        } catch (final KiwiError e) {
            Log.error(getClass(), "KiwiError creating object", e);
            HttpUtil.json(response, callback, 400, "{\"error\":\"" + e.getCode() + "\"}");
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
