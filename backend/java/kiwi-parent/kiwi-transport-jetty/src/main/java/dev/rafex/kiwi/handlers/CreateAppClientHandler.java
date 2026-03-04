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
import dev.rafex.ether.http.jetty12.JettyApiResponses;
import dev.rafex.ether.http.jetty12.JettyAuthHandler;
import dev.rafex.ether.http.jetty12.JettyHttpExchange;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;
import dev.rafex.ether.json.JsonUtils;
import dev.rafex.kiwi.security.JwtService;
import dev.rafex.kiwi.services.AppClientAuthService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;

import com.fasterxml.jackson.databind.JsonNode;

public final class CreateAppClientHandler extends NonBlockingResourceHandler {

	private static final JsonCodec JSON_CODEC = JsonUtils.codec();
	private static final JettyApiResponses RESPONSES = new JettyApiResponses(JSON_CODEC);
	private static final JettyApiErrorResponses ERRORS = new JettyApiErrorResponses(JSON_CODEC);

	private final AppClientAuthService appClientService;

	public CreateAppClientHandler(final AppClientAuthService appClientService) {
		super(JSON_CODEC);
		this.appClientService = Objects.requireNonNull(appClientService);
	}

	@Override
	protected String basePath() {
		return "/admin/app-clients";
	}

	@Override
	protected List<Route> routes() {
		return List.of(Route.of("/", Set.of("POST")));
	}

	@Override
	public Set<String> supportedMethods() {
		return Set.of("POST");
	}

	@Override
	public boolean post(final dev.rafex.ether.http.core.HttpExchange x) throws Exception {
		final var jx = asJetty(x);
		final var authObj = jx.request().getAttribute(JettyAuthHandler.REQ_ATTR_AUTH);
		if (!(authObj instanceof final JwtService.AuthContext ctx)) {
			ERRORS.unauthorized(jx.response(), jx.callback(), "missing_bearer_token");
			return true;
		}

		if (!"user".equalsIgnoreCase(ctx.tokenType()) || !ctx.roles().contains("ADMIN")) {
			ERRORS.forbidden(jx.response(), jx.callback(), "missing_admin_role");
			return true;
		}

		final String body;
		try {
			body = Content.Source.asString(jx.request(), StandardCharsets.UTF_8);
		} catch (final Exception e) {
			ERRORS.badRequest(jx.response(), jx.callback(), "cannot_read_body");
			return true;
		}

		if (body == null || body.isBlank()) {
			ERRORS.badRequest(jx.response(), jx.callback(), "missing_body");
			return true;
		}

		final JsonNode json;
		try {
			json = JSON_CODEC.readTree(body);
		} catch (final Exception e) {
			ERRORS.badRequest(jx.response(), jx.callback(), "invalid_json");
			return true;
		}

		final var clientId = text(json, "client_id");
		final var clientSecret = text(json, "client_secret");
		final var name = text(json, "name");
		final var roles = roles(json.get("roles"));

		if (clientId == null || clientSecret == null) {
			ERRORS.badRequest(jx.response(), jx.callback(), "missing_fields");
			return true;
		}

		final var res = appClientService.createClient(clientId, name, clientSecret.toCharArray(), roles);
		if (!res.ok()) {
			final var code = res.code() == null ? "error" : res.code();
			if ("client_id_taken".equals(code)) {
				ERRORS.error(jx.response(), jx.callback(), HttpStatus.CONFLICT_409, "conflict", "client_id_taken",
						"client id already exists");
			} else if ("invalid_input".equals(code)) {
				ERRORS.badRequest(jx.response(), jx.callback(), "invalid_input");
			} else {
				ERRORS.internalServerError(jx.response(), jx.callback(), code);
			}
			return true;
		}

		RESPONSES.json(jx.response(), jx.callback(), HttpStatus.CREATED_201,
				Map.of("app_client_id", res.appClientId().toString(), "client_id", res.clientId(), "name", res.name(),
						"roles", res.roles()));
		return true;
	}

	private static JettyHttpExchange asJetty(final dev.rafex.ether.http.core.HttpExchange x) {
		return (JettyHttpExchange) x;
	}

	private static String text(final JsonNode node, final String field) {
		if (node == null) {
			return null;
		}
		final var v = node.get(field);
		return v != null && v.isTextual() ? v.asText() : null;
	}

	private static List<String> roles(final JsonNode node) {
		if (node == null || node.isNull()) {
			return List.of();
		}
		if (node.isTextual()) {
			final var role = node.asText();
			return role == null || role.isBlank() ? List.of() : List.of(role);
		}
		if (!node.isArray()) {
			return List.of();
		}

		final var out = new ArrayList<String>();
		for (final var item : node) {
			if (item != null && item.isTextual()) {
				final var role = item.asText();
				if (role != null && !role.isBlank()) {
					out.add(role);
				}
			}
		}
		return out;
	}
}
