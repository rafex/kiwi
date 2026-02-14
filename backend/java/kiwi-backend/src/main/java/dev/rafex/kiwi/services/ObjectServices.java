package dev.rafex.kiwi.services;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.postgresql.util.PSQLException;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.rafex.kiwi.db.ObjectRepository;
import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.logging.Log;

public class ObjectServices {

    private final ObjectRepository repo;
    private final ObjectMapper om = new ObjectMapper();

    public ObjectServices(final ObjectRepository repo) {
        this.repo = repo;
    }

    public boolean search(final Request request, final Response response, final Callback callback) {
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

            final var rows = repo.search(q.trim(), tags, locationId, limit);

            final var out = om.createObjectNode();
            final var items = out.putArray("items");
            for (final var r : rows) {
                final var it = om.createObjectNode();
                it.put("object_id", r.objectId().toString());
                it.put("name", r.name());
                it.put("rank", r.rank());
                items.add(it);
            }

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

    public void create(final UUID objectId, final String name, final String description, final String type, final String[] tags, final String metadataJson, final UUID locationId) {
        try {
            repo.createObject(objectId, name, description, type, tags, metadataJson, locationId);
        } catch (final SQLException e) {
            Log.error(getClass(), "Error creating object in DB", e);
        }
    }

    public void move(final UUID objectId, final UUID newLocationId) throws KiwiError {
        try {
            repo.moveObject(objectId, newLocationId);
        } catch (final PSQLException e) {
            if ("23503".equals(e.getSQLState())) {
                throw new KiwiError("E-001", "newLocationId does not exist", e);
            }
            Log.error(getClass(), "DB error moving object", e);

        } catch (final SQLException e) {
            Log.error(getClass(), "Error moving object", e);
            throw new KiwiError("E-002", "SQLException", e);
        }

    }
}