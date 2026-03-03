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

import dev.rafex.kiwi.handlers.resources.HttpExchange;
import dev.rafex.kiwi.handlers.resources.NonBlockingResourceHandler;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.json.JsonUtil;
import dev.rafex.kiwi.security.JwtService;
import dev.rafex.kiwi.services.UserProvisioningService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;

import com.fasterxml.jackson.databind.JsonNode;

public final class CreateUserHandler extends NonBlockingResourceHandler {

	private static final boolean PROVISIONING_ENABLED = "true"
			.equalsIgnoreCase(System.getenv().getOrDefault("ENABLE_USER_PROVISIONING", "false"));

	private static final String ENV = System.getenv().getOrDefault("ENVIRONMENT", "unknown"); // ej: work02 | live02

	private static final String BOOTSTRAP_TOKEN = System.getenv().getOrDefault("BOOTSTRAP_TOKEN", "");

	private final UserProvisioningService provisioning;

	public CreateUserHandler(final UserProvisioningService provisioning) {
		this.provisioning = Objects.requireNonNull(provisioning);
	}

	@Override
	protected String basePath() {
		return "/admin/users";
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
	public boolean post(final HttpExchange x) throws Exception {

		if (!PROVISIONING_ENABLED || !isSandbox()) {
			HttpUtil.notFound(x.response(), x.callback(), x.request().getHttpURI().getPath());
			return true;
		}

		final var bootstrap = hasValidBootstrapToken(x.request());

		// Si no hay users -> solo permitimos BOOTSTRAP (para crear el primero)
		// Si ya hay users -> solo permitimos JWT admin
		final var existsAnyUser = provisioning.existsAnyUser();

		if (!existsAnyUser) {
			// primer usuario: debe venir bootstrap token
			if (!bootstrap) {
				HttpUtil.notFound(x.response(), x.callback(), x.request().getHttpURI().getPath());
				return true;
			}
			// si bootstrap ok, NO pedimos JWT (todavía no existe)
		} else {
			// ya hay usuarios: bootstrap ya NO debe servir
			if (bootstrap) {
				HttpUtil.notFound(x.response(), x.callback(), x.request().getHttpURI().getPath());
				return true;
			}

			// exige JWT admin
			final var authObj = x.request().getAttribute(JwtAuthHandler.REQ_ATTR_AUTH);
			if (authObj == null) {
				// para ocultar endpoint
				HttpUtil.notFound(x.response(), x.callback(), x.request().getHttpURI().getPath());
				return true;
			}

			if (authObj instanceof final JwtService.AuthContext ctx && !ctx.roles().contains("ADMIN")) {
				HttpUtil.forbidden(x.response(), x.callback(), "missing_admin_role");
				return true;
			}
		}

		// ---- Body ----
		final String body;
		try {
			body = Content.Source.asString(x.request(), StandardCharsets.UTF_8);
		} catch (final Exception e) {
			HttpUtil.badRequest(x.response(), x.callback(), "cannot_read_body");
			return true;
		}

		if (body == null || body.isBlank()) {
			HttpUtil.badRequest(x.response(), x.callback(), "missing_body");
			return true;
		}

		final JsonNode json;
		try {
			json = JsonUtil.MAPPER.readTree(body);
		} catch (final Exception e) {
			HttpUtil.badRequest(x.response(), x.callback(), "invalid_json");
			return true;
		}

		final var username = text(json, "username");
		final var password = text(json, "password");
		final var roles = roles(json.get("roles"));

		if (username == null || username.isBlank() || password == null || password.isBlank()) {
			HttpUtil.badRequest(x.response(), x.callback(), "missing_fields");
			return true;
		}

		final var res = provisioning.createUser(username, password.toCharArray(), roles);

		if (!res.ok()) {
			final var code = res.code();
			if ("username_taken".equals(code)) {
				HttpUtil.json(x.response(), x.callback(), HttpStatus.CONFLICT_409,
						Map.of("error", "conflict", "code", "username_taken"));
			} else if ("invalid_input".equals(code)) {
				HttpUtil.badRequest(x.response(), x.callback(), "invalid_input");
			} else {
				HttpUtil.json(x.response(), x.callback(), HttpStatus.INTERNAL_SERVER_ERROR_500,
						Map.of("error", "server_error", "code", code));
			}
			return true;
		}

		HttpUtil.ok(x.response(), x.callback(),
				Map.of("user_id", res.userId().toString(), "username", username, "roles", roles));
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
			// permite "roles":"admin"
			final var s = node.asText();
			return s == null || s.isBlank() ? List.of() : List.of(s);
		}

		if (!node.isArray()) {
			return List.of();
		}

		final var out = new ArrayList<String>();
		for (final var it : node) {
			if (it != null && it.isTextual()) {
				final var s = it.asText();
				if (s != null && !s.isBlank()) {
					out.add(s);
				}
			}
		}
		return out;
	}

	private static boolean hasValidBootstrapToken(final Request request) {
		final var t = request.getHeaders().get("x-bootstrap-token");
		return t != null && !BOOTSTRAP_TOKEN.isBlank() && MessageDigest.isEqual(t.getBytes(StandardCharsets.UTF_8),
				BOOTSTRAP_TOKEN.getBytes(StandardCharsets.UTF_8));
	}

	private static boolean isSandbox() {
		// ajusta a tus nombres reales
		return "work02".equalsIgnoreCase(ENV) || "sandbox".equalsIgnoreCase(ENV) || "dev".equalsIgnoreCase(ENV);
	}
}
