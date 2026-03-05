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

import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.JettyApiErrorResponses;
import dev.rafex.kiwi.dtos.CreateLocationRequest;
import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.ether.http.jetty12.JettyHttpExchange;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;
import dev.rafex.ether.json.JsonUtils;
import dev.rafex.kiwi.http.KiwiErrorHttpMapper;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.services.LocationService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LocationHandler extends NonBlockingResourceHandler {

	private static final JsonCodec JSON_CODEC = JsonUtils.codec();
	private static final JettyApiErrorResponses ERRORS = new JettyApiErrorResponses(JSON_CODEC);

	private final LocationService service;

	public LocationHandler(final LocationService services) {
		super(JSON_CODEC);
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
	public boolean post(final dev.rafex.ether.http.core.HttpExchange x) {
		Log.info(getClass(), "Handling location creation request");
		return create(asJetty(x));
	}

	@Override
	public Set<String> supportedMethods() {
		return Set.of("POST");
	}

	private boolean create(final JettyHttpExchange x) {
		try {

			final var r = JSON_CODEC.readValue(org.eclipse.jetty.server.Request.asInputStream(x.request()),
					CreateLocationRequest.class);

			if (r.name() == null || r.name().isBlank()) {
				ERRORS.badRequest(x.response(), x.callback(), "name is required");
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
			ERRORS.badRequest(x.response(), x.callback(), "invalid UUID in parentLocationId");
			return true;

		} catch (final KiwiError e) {
			final var mapped = KiwiErrorHttpMapper.map(e, "location.create");
			ERRORS.error(x.response(), x.callback(), mapped.status(), mapped.error(), mapped.code(), mapped.message(),
					x.path());
			return true;

		} catch (final Exception e) {
			Log.debug(getClass(), "Error creating location: {}", e.getMessage());
			ERRORS.internalServerError(x.response(), x.callback(), "internal_error");
			return true;
		}
	}

	private static JettyHttpExchange asJetty(final dev.rafex.ether.http.core.HttpExchange x) {
		return (JettyHttpExchange) x;
	}

}
