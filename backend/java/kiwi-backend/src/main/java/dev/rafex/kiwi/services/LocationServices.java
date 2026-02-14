package dev.rafex.kiwi.services;

import java.util.UUID;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.postgresql.util.PSQLException;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.rafex.kiwi.db.LocationRepository;
import dev.rafex.kiwi.handlers.dto.CreateLocationRequest;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.logging.Log;

public class LocationServices {

    private final LocationRepository repo;
    private final ObjectMapper om = new ObjectMapper();

    public LocationServices(final LocationRepository repo) {
        this.repo = repo;
    }

    public boolean create(final Request request, final Response response, final Callback callback) {
        try {
            final var body = Request.asInputStream(request).readAllBytes();
            final var r = om.readValue(body, CreateLocationRequest.class);

            if (r.getName() == null || r.getName().isBlank()) {
                HttpUtil.badRequest(response, callback, "name is required");
                return true;
            }

            UUID parentId = null;
            if (r.getParentLocationId() != null && !r.getParentLocationId().isBlank()) {
                parentId = UUID.fromString(r.getParentLocationId());
            }

            final var locationId = UUID.randomUUID();
            repo.createLocation(locationId, r.getName().trim(), parentId);

            HttpUtil.json(response, callback, 201, "{\"location_id\":\"" + locationId + "\"}");
            return true;

        } catch (final IllegalArgumentException e) {
            HttpUtil.badRequest(response, callback, "invalid UUID in parentLocationId");
            return true;

        } catch (final PSQLException e) {
            // FK violation: parent_location_id no existe
            if ("23503".equals(e.getSQLState())) {
                HttpUtil.badRequest(response, callback, "parentLocationId does not exist");
                return true;
            }
            Log.debug(getClass(), "DB error creating location: {} ", e.getMessage());
            HttpUtil.json(response, callback, 500, "{\"error\":\"db_error\"}");
            return true;

        } catch (final Exception e) {
            Log.debug(getClass(), "Error creating location: {}", e.getMessage());
            HttpUtil.json(response, callback, 500, "{\"error\":\"internal_error\"}");
            return true;
        }
    }
}
