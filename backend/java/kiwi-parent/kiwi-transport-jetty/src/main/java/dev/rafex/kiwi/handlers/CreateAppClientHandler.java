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

import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.json.JsonUtil;
import dev.rafex.kiwi.security.JwtService;
import dev.rafex.kiwi.services.AppClientAuthService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.fasterxml.jackson.databind.JsonNode;

public final class CreateAppClientHandler extends Handler.Abstract.NonBlocking {

	private final AppClientAuthService appClientService;

	public CreateAppClientHandler(final AppClientAuthService appClientService) {
		this.appClientService = Objects.requireNonNull(appClientService);
	}

	@Override
	public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			HttpUtil.json(response, callback, HttpStatus.METHOD_NOT_ALLOWED_405, Map.of("error", "method_not_allowed"));
			return true;
		}

		final var authObj = request.getAttribute(JwtAuthHandler.REQ_ATTR_AUTH);
		if (!(authObj instanceof final JwtService.AuthContext ctx)) {
			HttpUtil.unauthorized(response, callback, "missing_bearer_token");
			return true;
		}

		if (!"user".equalsIgnoreCase(ctx.tokenType()) || !ctx.roles().contains("ADMIN")) {
			HttpUtil.forbidden(response, callback, "missing_admin_role");
			return true;
		}

		final String body;
		try {
			body = Content.Source.asString(request, StandardCharsets.UTF_8);
		} catch (final Exception e) {
			HttpUtil.badRequest(response, callback, "cannot_read_body");
			return true;
		}

		if (body == null || body.isBlank()) {
			HttpUtil.badRequest(response, callback, "missing_body");
			return true;
		}

		final JsonNode json;
		try {
			json = JsonUtil.MAPPER.readTree(body);
		} catch (final Exception e) {
			HttpUtil.badRequest(response, callback, "invalid_json");
			return true;
		}

		final var clientId = text(json, "client_id");
		final var clientSecret = text(json, "client_secret");
		final var name = text(json, "name");
		final var roles = roles(json.get("roles"));

		if (clientId == null || clientSecret == null) {
			HttpUtil.badRequest(response, callback, "missing_fields");
			return true;
		}

		final var res = appClientService.createClient(clientId, name, clientSecret.toCharArray(), roles);
		if (!res.ok()) {
			final var code = res.code() == null ? "error" : res.code();
			if ("client_id_taken".equals(code)) {
				HttpUtil.json(response, callback, HttpStatus.CONFLICT_409,
						Map.of("error", "conflict", "code", "client_id_taken"));
			} else if ("invalid_input".equals(code)) {
				HttpUtil.badRequest(response, callback, "invalid_input");
			} else {
				HttpUtil.internalServerError(response, callback, code);
			}
			return true;
		}

		HttpUtil.json(response, callback, HttpStatus.CREATED_201,
				Map.of("app_client_id", res.appClientId().toString(), "client_id", res.clientId(), "name", res.name(),
						"roles", res.roles()));
		return true;
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
