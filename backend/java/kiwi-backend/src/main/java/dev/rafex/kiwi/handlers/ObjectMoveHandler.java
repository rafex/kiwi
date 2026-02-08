package dev.rafex.kiwi.handlers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.postgresql.util.PSQLException;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.rafex.kiwi.db.ObjectRepository;
import dev.rafex.kiwi.handlers.dto.MoveObjectRequest;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.logging.Log;

public class ObjectMoveHandler {

	private final ObjectRepository repo;
	private final ObjectMapper om = new ObjectMapper();

	public ObjectMoveHandler(final ObjectRepository repo) {
		this.repo = repo;
	}

	public boolean handle(final Request request, final Response response, final Callback callback,
			final UUID objectId) {
		try {
			Log.info(ObjectMoveHandler.class, "Handling object move request");

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

			repo.moveObject(objectId, newLocationId);

			// 204 No Content
			response.setStatus(204);
			callback.succeeded();
			return true;

		} catch (final PSQLException e) {
			// FK violation: new location no existe
			if ("23503".equals(e.getSQLState())) {
				HttpUtil.badRequest(response, callback, "newLocationId does not exist");
				return true;
			}
			Log.error(ObjectMoveHandler.class, "DB error moving object", e);
			HttpUtil.json(response, callback, 500, "{\"error\":\"db_error\"}");
			return true;

		} catch (final Exception e) {
			Log.error(ObjectMoveHandler.class, "Error moving object", e);
			HttpUtil.json(response, callback, 500, "{\"error\":\"internal_error\"}");
			return true;
		}
	}
}
