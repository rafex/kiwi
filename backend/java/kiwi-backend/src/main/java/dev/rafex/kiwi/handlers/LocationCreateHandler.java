package dev.rafex.kiwi.handlers;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.postgresql.util.PSQLException;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.rafex.kiwi.db.LocationRepository;
import dev.rafex.kiwi.handlers.dto.CreateLocationRequest;
import dev.rafex.kiwi.http.HttpUtil;

public class LocationCreateHandler {
	private static final Logger LOG = Logger.getLogger(LocationCreateHandler.class.getName());

	private final LocationRepository repo;
	private final ObjectMapper om = new ObjectMapper();

	public LocationCreateHandler(final LocationRepository repo) {
		this.repo = repo;
	}

	public boolean handle(final Request request, final Response response, final Callback callback) {
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
			LOG.severe("DB error creating location: " + e.getMessage());
			HttpUtil.json(response, callback, 500, "{\"error\":\"db_error\"}");
			return true;

		} catch (final Exception e) {
			LOG.severe("Error creating location: " + e.getMessage());
			HttpUtil.json(response, callback, 500, "{\"error\":\"internal_error\"}");
			return true;
		}
	}
}
