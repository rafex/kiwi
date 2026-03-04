/*
 * Copyright 2026 Raúl Eduardo González Argote
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.rafex.kiwi.handlers;

import dev.rafex.kiwi.dtos.CreateObjectRequest;
import dev.rafex.kiwi.dtos.FuzzyResponse;
import dev.rafex.kiwi.dtos.MoveObjectRequest;
import dev.rafex.kiwi.dtos.SearchResponse;
import dev.rafex.kiwi.dtos.UpdateTagsRequest;
import dev.rafex.kiwi.dtos.UpdateTextRequest;
import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.handlers.resources.HttpExchange;
import dev.rafex.kiwi.handlers.resources.NonBlockingResourceHandler;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.http.KiwiErrorHttpMapper;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.query.QuerySpecBuilder;
import dev.rafex.kiwi.services.ObjectService;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ObjectHandler extends NonBlockingResourceHandler {

	private final ObjectService service;
	private final QuerySpecBuilder querySpecBuilder;

	public ObjectHandler(final ObjectService service) {
		this.service = service;
		this.querySpecBuilder = new QuerySpecBuilder();
	}

	@Override
	protected String basePath() {
		return "/objects";
	}

	@Override
	protected List<Route> routes() {
		return List.of(
				Route.of("/search", Set.of("GET")),
				Route.of("/fuzzy", Set.of("GET")),
				Route.of("/{id}/move", Set.of("PATCH")),
				Route.of("/{id}/tags", Set.of("PATCH")),
				Route.of("/{id}/text", Set.of("PATCH")),
				Route.of("/{id}", Set.of("GET")),
				Route.of("/", Set.of("POST")));
	}

	@Override
	public boolean post(final HttpExchange x) {
		Log.info(getClass(), "Handling object creation request");
		return create(x);
	}

	@Override
	public boolean get(final HttpExchange x) {
		final var path = x.path();
		if (path.endsWith("/search")) {
			return search(x);
		}
		if (path.endsWith("/fuzzy")) {
			return fuzzySearch(x);
		}
		final var id = x.pathParam("id");
		if (id == null) {
			HttpUtil.notFound(x.response(), x.callback());
			return true;
		}
		return byId(x, parseUuid(id, "invalid UUID in path"));
	}

	@Override
	public boolean patch(final HttpExchange x) {
		final var id = x.pathParam("id");
		if (id == null) {
			HttpUtil.notFound(x.response(), x.callback());
			return true;
		}
		final var objectId = parseUuid(id, "invalid UUID in path");
		final var path = x.path();
		if (path.endsWith("/move")) {
			return moveLocation(x, objectId);
		}
		if (path.endsWith("/tags")) {
			return updateTags(x, objectId);
		}
		if (path.endsWith("/text")) {
			return updateText(x, objectId);
		}
		HttpUtil.notFound(x.response(), x.callback());
		return true;
	}

	@Override
	public Set<String> supportedMethods() {
		return Set.of("GET", "POST", "PATCH");
	}

	private boolean byId(final HttpExchange x, final UUID objectId) {
		try {
			final var objectOpt = service.getById(objectId);
			if (objectOpt.isEmpty()) {
				HttpUtil.notFound(x.response(), x.callback());
				return true;
			}

			final var object = objectOpt.get();
			final var responseBody = new LinkedHashMap<String, Object>();
			responseBody.put("object_id", object.objectId());
			responseBody.put("name", object.name());
			responseBody.put("description", object.description());
			responseBody.put("type", object.type());
			responseBody.put("status", object.status());
			responseBody.put("current_location_id", object.currentLocationId());
			responseBody.put("tags", object.tags() == null ? List.of() : Arrays.asList(object.tags()));
			if (object.metadataJson() == null || object.metadataJson().isBlank()) {
				responseBody.put("metadata", null);
			} else {
				responseBody.put("metadata", HttpUtil.jsonCodec().readTree(object.metadataJson()));
			}
			responseBody.put("created_at", object.createdAt());
			responseBody.put("updated_at", object.updatedAt());

			x.json(200, responseBody);
			return true;
		} catch (final Exception e) {
			Log.error(getClass(), "Error getting object by id", e);
			HttpUtil.internalServerError(x.response(), x.callback(), "internal_error");
			return true;
		}
	}

	private boolean search(final HttpExchange x) {
		try {
			final var spec = querySpecBuilder.fromRawParams(
					queryParam(x, "q"),
					queryParam(x, "tags"),
					queryParam(x, "locationId"),
					queryParam(x, "enabled"),
					queryParam(x, "sort"),
					queryParam(x, "limit"),
					queryParam(x, "offset"));

			final var search = service.search(spec);
			x.json(200, new SearchResponse(search, spec.limit(), spec.offset()));
			return true;
		} catch (final IllegalArgumentException e) {
			HttpUtil.badRequest(x.response(), x.callback(), e.getMessage());
			return true;
		} catch (final Exception e) {
			Log.error(getClass(), "Error searching objects", e);
			HttpUtil.internalServerError(x.response(), x.callback(), "internal_error");
			return true;
		}
	}

	private boolean moveLocation(final HttpExchange x, final UUID objectId) {
		try {
			Log.info(getClass(), "Handling object move request");
			final var r = HttpUtil.jsonCodec().readValue(org.eclipse.jetty.server.Request.asInputStream(x.request()),
					MoveObjectRequest.class);

			if (r.newLocationId() == null || r.newLocationId().isBlank()) {
				HttpUtil.badRequest(x.response(), x.callback(), "newLocationId is required");
				return true;
			}

			final UUID newLocationId;
			try {
				newLocationId = UUID.fromString(r.newLocationId());
			} catch (final IllegalArgumentException e) {
				HttpUtil.badRequest(x.response(), x.callback(), "invalid UUID in newLocationId");
				return true;
			}

			service.move(objectId, newLocationId);
			HttpUtil.ok_noContent(x.response(), x.callback());
			return true;
		} catch (final KiwiError e) {
			Log.error(getClass(), "KiwiError moving object", e);
			HttpUtil.error(x.response(), x.callback(), KiwiErrorHttpMapper.map(e, "object.move"));
			return true;
		} catch (final RuntimeException e) {
			Log.error(getClass(), "Error moving object", e);
			HttpUtil.badRequest(x.response(), x.callback(), "invalid request body");
			return true;
		}
	}

	private boolean create(final HttpExchange x) {
		try {
			final var r = HttpUtil.jsonCodec().readValue(org.eclipse.jetty.server.Request.asInputStream(x.request()),
					CreateObjectRequest.class);

			if (r.name() == null || r.name().isBlank()) {
				HttpUtil.badRequest(x.response(), x.callback(), "name is required");
				return true;
			}
			if (r.locationId() == null || r.locationId().isBlank()) {
				HttpUtil.badRequest(x.response(), x.callback(), "locationId is required");
				return true;
			}

			final var objectId = UUID.randomUUID();
			final var locationId = UUID.fromString(r.locationId());
			final var tags = r.tags() == null ? null : r.tags().toArray(new String[0]);
			final var metadataJson = r.metadata() == null ? null : HttpUtil.jsonCodec().toJson(r.metadata());

			service.create(objectId, r.name(), r.description(), r.type(), tags, metadataJson, locationId);
			x.json(201, "{\"object_id\":\"" + objectId + "\"}");
			return true;
		} catch (final KiwiError e) {
			Log.error(getClass(), "KiwiError creating object", e);
			HttpUtil.error(x.response(), x.callback(), KiwiErrorHttpMapper.map(e, "object.create"));
			return true;
		} catch (final IllegalArgumentException e) {
			Log.error(getClass(), "Invalid UUID format", e);
			HttpUtil.badRequest(x.response(), x.callback(), "invalid UUID");
			return true;
		} catch (final Exception e) {
			Log.error(getClass(), "Error creating object", e);
			HttpUtil.internalServerError(x.response(), x.callback(), "internal_error");
			return true;
		}
	}

	private boolean updateTags(final HttpExchange x, final UUID objectId) {
		try {
			Log.info(getClass(), "Handling object tag update request");
			final var r = HttpUtil.jsonCodec().readValue(org.eclipse.jetty.server.Request.asInputStream(x.request()),
					UpdateTagsRequest.class);

			if (r.tags() == null) {
				HttpUtil.badRequest(x.response(), x.callback(), "tags is required");
				return true;
			}

			final var tags = r.tags().toArray(String[]::new);
			service.updateTags(objectId, tags);
			HttpUtil.ok_noContent(x.response(), x.callback());
			return true;
		} catch (final KiwiError e) {
			Log.error(getClass(), "KiwiError updating tags", e);
			HttpUtil.error(x.response(), x.callback(), KiwiErrorHttpMapper.map(e, "object.update_tags"));
			return true;
		} catch (final RuntimeException e1) {
			Log.error(getClass(), "Error updating tags", e1);
			HttpUtil.badRequest(x.response(), x.callback(), "invalid request body");
			return true;
		}
	}

	private boolean updateText(final HttpExchange x, final UUID objectId) {
		try {
			Log.info(getClass(), "Handling object text update request");
			final var r = HttpUtil.jsonCodec().readValue(org.eclipse.jetty.server.Request.asInputStream(x.request()),
					UpdateTextRequest.class);

			if ((r.name() == null || r.name().isBlank()) && (r.description() == null || r.description().isBlank())) {
				HttpUtil.badRequest(x.response(), x.callback(), "name or description is required");
				return true;
			}

			service.updateText(objectId, r.name(), r.description());
			HttpUtil.ok_noContent(x.response(), x.callback());
			return true;
		} catch (final KiwiError e) {
			Log.error(getClass(), "KiwiError updating text", e);
			HttpUtil.error(x.response(), x.callback(), KiwiErrorHttpMapper.map(e, "object.update_text"));
			return true;
		} catch (final RuntimeException e1) {
			Log.error(getClass(), "Error updating text", e1);
			HttpUtil.badRequest(x.response(), x.callback(), "invalid request body");
			return true;
		}
	}

	private boolean fuzzySearch(final HttpExchange x) {
		try {
			Log.info(getClass(), "Handling object fuzzy search request");
			final var name = queryParam(x, "name");
			final var limitParam = queryParam(x, "limit");
			final var offsetParam = queryParam(x, "offset");
			if (name == null || name.isBlank()) {
				HttpUtil.badRequest(x.response(), x.callback(), "name is required");
				return true;
			}
			final var limit = parseLimit(limitParam, 20, 1, 200);
			final var offset = parseOffset(offsetParam, 0, 0, 100_000);

			final var search = service.fuzzy(name, limit, offset);
			x.json(200, new FuzzyResponse(search, limit, offset));
			return true;
		} catch (final Exception e) {
			Log.error(getClass(), "Error handling fuzzy search", e);
			HttpUtil.internalServerError(x.response(), x.callback(), "internal_error");
			return true;
		}
	}

	private static UUID parseUuid(final String raw, final String errorMessage) {
		try {
			return UUID.fromString(raw);
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException(errorMessage);
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

	private static int parseOffset(final String offsetStr, final int def, final int min, final int max) {
		if (offsetStr == null || offsetStr.trim().isEmpty()) {
			return def;
		}
		try {
			final var v = Integer.parseInt(offsetStr.trim());
			if (v < min) {
				return min;
			}
			if (v > max) {
				return max;
			}
			return v;
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException("offset must be an integer");
		}
	}

}
