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

	public ObjectCreateHandler(ObjectRepository repo) {
		this.repo = repo;
	}

	public boolean handle(Request request, Response response, Callback callback) {
		try {
			byte[] bytes = Request.asInputStream(request).readAllBytes();
			String body = new String(bytes, StandardCharsets.UTF_8);

			CreateObjectRequest r = om.readValue(body, CreateObjectRequest.class);

			// validaciones mínimas
			if (r.getName() == null || r.getName().isBlank()) {
				HttpUtil.badRequest(response, callback, "name is required");
				return true;
			}
			if (r.getLocationId() == null || r.getLocationId().isBlank()) {
				HttpUtil.badRequest(response, callback, "locationId is required");
				return true;
			}

			UUID objectId = UUID.randomUUID();
			UUID locationId = UUID.fromString(r.getLocationId());

			String[] tags = (r.getTags() == null) ? null : r.getTags().toArray(new String[0]);
			String metadataJson = (r.getMetadata() == null) ? null : om.writeValueAsString(r.getMetadata());

			repo.createObject(objectId, r.getName(), r.getDescription(), r.getType(), tags, metadataJson, locationId);

			HttpUtil.json(response, callback, 201, "{\"object_id\":\"" + objectId + "\"}");
			return true;

		} catch (IllegalArgumentException e) {
			Log.error(getClass(), "Invalid UUID format", e);
			HttpUtil.badRequest(response, callback, "invalid UUID");
			return true;
		} catch (Exception e) {
			Log.error(getClass(), "Error creating object", e);
			HttpUtil.json(response, callback, 500, "{\"error\":\"internal_error\"}");
			return true;
		}
	}
}