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
import dev.rafex.kiwi.handlers.resources.HttpExchange;
import dev.rafex.kiwi.handlers.resources.NonBlockingResourceHandler;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.http.KiwiErrorHttpMapper;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.services.LocationService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LocationHandler extends NonBlockingResourceHandler {

	private final LocationService service;

	public LocationHandler(final LocationService services) {
		service = services;
	}

	@Override
	protected String basePath() {
		return "/locations";
	}

	@Override
	protected List<Route> routes() {
		return List.of(Route.of("/", Set.of("POST")));
	}

	@Override
	public boolean post(final HttpExchange x) {
		Log.info(getClass(), "Handling location creation request");
		return create(x);
	}

	@Override
	public Set<String> supportedMethods() {
		return Set.of("POST");
	}

	private boolean create(final HttpExchange x) {
		try {

			final var r = HttpUtil.jsonCodec().readValue(org.eclipse.jetty.server.Request.asInputStream(x.request()),
					CreateLocationRequest.class);

			if (r.name() == null || r.name().isBlank()) {
				HttpUtil.badRequest(x.response(), x.callback(), "name is required");
				return true;
			}

			UUID parentId = null;
			if (r.parentLocationId() != null && !r.parentLocationId().isBlank()) {
				parentId = UUID.fromString(r.parentLocationId());
			}

			final var locationId = UUID.randomUUID();
			service.create(locationId, r.name().trim(), parentId);

			x.json(201, "{\"location_id\":\"" + locationId + "\"}");
			return true;

		} catch (final IllegalArgumentException e) {
			HttpUtil.badRequest(x.response(), x.callback(), "invalid UUID in parentLocationId");
			return true;

		} catch (final KiwiError e) {
			HttpUtil.error(x.response(), x.callback(), KiwiErrorHttpMapper.map(e, "location.create"));
			return true;

		} catch (final Exception e) {
			Log.debug(getClass(), "Error creating location: {}", e.getMessage());
			HttpUtil.internalServerError(x.response(), x.callback(), "internal_error");
			return true;
		}
	}

}
