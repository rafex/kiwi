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

import dev.rafex.kiwi.dtos.CreateLocationRequest;
import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.json.JsonUtil;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.services.LocationService;

import java.util.UUID;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LocationHandler extends Handler.Abstract {

	private final LocationService service;
	private final ObjectMapper om;

	public LocationHandler(final LocationService services) {
		service = services;
		om = JsonUtil.MAPPER;
	}

	@Override
	public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
		final var method = request.getMethod();
		final var path = request.getHttpURI().getPath();

		if ("POST".equals(method) && "/locations".equals(path)) {
			Log.info(getClass(), "Handling object creation request");
			return create(request, response, callback);
		}
		return false;
	}

	private boolean create(final Request request, final Response response, final Callback callback) {
		try {

			final var r = om.readValue(Request.asInputStream(request), CreateLocationRequest.class);

			if (r.name() == null || r.name().isBlank()) {
				HttpUtil.badRequest(response, callback, "name is required");
				return true;
			}

			UUID parentId = null;
			if (r.parentLocationId() != null && !r.parentLocationId().isBlank()) {
				parentId = UUID.fromString(r.parentLocationId());
			}

			final var locationId = UUID.randomUUID();
			service.create(locationId, r.name().trim(), parentId);

			HttpUtil.json(response, callback, 201, "{\"location_id\":\"" + locationId + "\"}");
			return true;

		} catch (final IllegalArgumentException e) {
			HttpUtil.badRequest(response, callback, "invalid UUID in parentLocationId");
			return true;

		} catch (final KiwiError e) {
			HttpUtil.json(response, callback, 500, "{\"error\":\"db_error\"}");
			return true;

		} catch (final Exception e) {
			Log.debug(getClass(), "Error creating location: {}", e.getMessage());
			HttpUtil.json(response, callback, 500, "{\"error\":\"internal_error\"}");
			return true;
		}
	}

}
