package dev.rafex.kiwi.handlers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.rafex.kiwi.db.ObjectRepository;
import dev.rafex.kiwi.handlers.dto.CreateObjectRequest;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.logging.Log;

public class ObjectCreateHandler {

	private final ObjectRepository repo;
	private final ObjectMapper om = new ObjectMapper();

	public ObjectCreateHandler(final ObjectRepository repo) {
		this.repo = repo;
	}

	public boolean handle(final Request request, final Response response, final Callback callback) {
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

			repo.createObject(objectId, r.getName(), r.getDescription(), r.getType(), tags, metadataJson, locationId);

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