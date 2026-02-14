package dev.rafex.kiwi.handlers;

import java.util.Map;
import java.util.UUID;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.rafex.kiwi.db.Db;
import dev.rafex.kiwi.db.LocationRepository;
import dev.rafex.kiwi.dtos.CreateLocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.json.JsonUtil;
import dev.rafex.kiwi.services.LocationServices;

public class LocationHandler extends Handler.Abstract {

    private static final Logger LOG = LoggerFactory.getLogger(LocationHandler.class);
    private final LocationServices services;
    private final ObjectMapper om = JsonUtil.MAPPER;

    public LocationHandler(final LocationServices services) {
        this.services = services;
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
        final var method = request.getMethod();
        final var path = request.getHttpURI().getPath();

        if ("POST".equals(method) && "/locations".equals(path)) {
            LOG.info("Handling object creation request");
            return create(request, response, callback);
        }
        return false;
    }

    private boolean create(final Request request, final Response response, final Callback callback) {
        try {
            final var body = Request.asInputStream(request).readAllBytes();
            final var r = om.readValue(body, CreateLocationRequest.class);

            if (r.name() == null || r.name().isBlank()) {
                HttpUtil.badRequest(response, callback, "name is required");
                return true;
            }

            UUID parentId = null;
            if (r.parentLocationId() != null && !r.parentLocationId().isBlank()) {
                parentId = UUID.fromString(r.parentLocationId());
            }

            final var locationId = UUID.randomUUID();
            services.create(locationId, r.name().trim(), parentId);

            HttpUtil.json(response, callback, 201, Map.of("location_id", locationId));
            return true;

        } catch (final IllegalArgumentException e) {
            HttpUtil.badRequest(response, callback, "invalid UUID in parentLocationId");
            return true;

        } catch (final KiwiError e) {
            HttpUtil.json(response, callback, 500, Map.of("error", "db_error"));
            return true;

        } catch (final Exception e) {
            LOG.debug("Error creating location: {}", e.getMessage());
            HttpUtil.json(response, callback, 500, Map.of("error", "internal_error"));
            return true;
        }
    }

}
