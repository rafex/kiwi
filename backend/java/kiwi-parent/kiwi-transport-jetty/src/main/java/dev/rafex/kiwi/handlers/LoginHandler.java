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
import dev.rafex.ether.http.jetty12.JettyHttpExchange;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;
import dev.rafex.ether.json.JsonUtils;
import dev.rafex.kiwi.security.JwtService;
import dev.rafex.kiwi.services.AuthService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jetty.io.Content;

import com.fasterxml.jackson.databind.JsonNode;

public final class LoginHandler extends NonBlockingResourceHandler {

	private static final JsonCodec JSON_CODEC = JsonUtils.codec();
	private static final JettyApiResponses RESPONSES = new JettyApiResponses(JSON_CODEC);
	private static final JettyApiErrorResponses ERRORS = new JettyApiErrorResponses(JSON_CODEC);

	private final JwtService jwt;
	private final AuthService authService;
	private final long ttlSeconds;

	public LoginHandler(final JwtService jwt, final AuthService authService) {
		this(jwt, authService, Long.parseLong(System.getenv().getOrDefault("JWT_TTL_SECONDS", "3600")));
	}

	public LoginHandler(final JwtService jwt, final AuthService authService, final long ttlSeconds) {
		super(JSON_CODEC);
		this.jwt = Objects.requireNonNull(jwt);
		this.authService = Objects.requireNonNull(authService);
		this.ttlSeconds = ttlSeconds;
	}

	@Override
	protected String basePath() {
		return "/auth/login";
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
		// 1) Intenta Basic Auth
		final var authz = jx.request().getHeaders().get("authorization");
		if (authz != null && authz.regionMatches(true, 0, "Basic ", 0, "Basic ".length())) {
			final var creds = decodeBasic(authz.substring("Basic ".length()).trim());
			if (creds == null) {
				ERRORS.unauthorized(jx.response(), jx.callback(), "bad_basic_auth");
				return true;
			}
			return authenticateAndMint(jx, creds.user, creds.pass);
		}

		// 2) JSON body: {"username":"...","password":"..."}
		final String body;
		try {
			body = Content.Source.asString(jx.request(), StandardCharsets.UTF_8);
		} catch (final Exception e) {
			ERRORS.badRequest(jx.response(), jx.callback(), "cannot_read_body");
			return true;
		}

		if (body == null || body.isBlank()) {
			ERRORS.unauthorized(jx.response(), jx.callback(), "missing_credentials");
			return true;
		}

		final JsonNode json;
		try {
			json = JSON_CODEC.readTree(body);
		} catch (final Exception e) {
			ERRORS.badRequest(jx.response(), jx.callback(), "invalid_json");
			return true;
		}

		final var user = text(json, "username");
		final var pass = text(json, "password");

		if (user == null || pass == null) {
			ERRORS.unauthorized(jx.response(), jx.callback(), "missing_credentials");
			return true;
		}

		return authenticateAndMint(jx, user, pass);
	}

	private boolean authenticateAndMint(final JettyHttpExchange x, final String username,
			final String password) throws Exception {

		// Nota: pasamos char[] para poder limpiarlo dentro de AuthServiceImpl
		final var result = authService.authenticate(username, password.toCharArray());

		if (!result.ok()) {
			// Mantén esto simple (evita user enumeration). "user_disabled" sí es útil
			// diferenciar.
			final var code = result.code() != null ? result.code() : "bad_credentials";

			if ("user_disabled".equals(code)) {
				ERRORS.forbidden(x.response(), x.callback(), "user_disabled");
			} else if ("bad_credentials".equals(code)) {
				ERRORS.unauthorized(x.response(), x.callback(), "bad_credentials");
			} else {
				ERRORS.unauthorized(x.response(), x.callback(), code);
			}
			return true;
		}

		// Recomendación: sub = userId (estable)
		final var subject = result.userId().toString();
		final var roles = result.roles(); // si quieres incluir roles en el token, aquí es donde los obtienes (si no los
											// tienes ya en el contexto de autenticación)

		// Si tu JwtService actual solo soporta mint(sub, ttl), deja esto así.
		// Si lo extiendes para roles/username, aquí es donde lo pasas.
		final var token = jwt.mint(subject, roles, ttlSeconds);

		RESPONSES.ok(x.response(), x.callback(),
				Map.of("token_type", "Bearer", "access_token", token, "expires_in", ttlSeconds
		// si quieres devolver roles al cliente:
		// "roles", result.roles()
		));
		return true;
	}

	private static String text(final JsonNode node, final String field) {
		final var v = node.get(field);
		return v != null && v.isTextual() ? v.asText() : null;
	}

	private record BasicCreds(String user, String pass) {
	}

	private static BasicCreds decodeBasic(final String base64Part) {
		try {
			final var decoded = new String(Base64.getDecoder().decode(base64Part), StandardCharsets.UTF_8);
			final var idx = decoded.indexOf(':');
			if (idx <= 0) {
				return null;
			}
			final var user = decoded.substring(0, idx);
			final var pass = decoded.substring(idx + 1);
			return new BasicCreds(user, pass);
		} catch (final Exception e) {
			return null;
		}
	}

	private static JettyHttpExchange asJetty(final dev.rafex.ether.http.core.HttpExchange x) {
		return (JettyHttpExchange) x;
	}
}
