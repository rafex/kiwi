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
import dev.rafex.kiwi.security.InMemoryRateLimiter;
import dev.rafex.kiwi.security.KiwiJwtService;
import dev.rafex.kiwi.services.AuthService;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

public final class LoginHandler extends NonBlockingResourceHandler {

	private static final JsonCodec JSON_CODEC = JsonUtils.codec();
	private static final JettyApiResponses RESPONSES = new JettyApiResponses(JSON_CODEC);
	private static final JettyApiErrorResponses ERRORS = new JettyApiErrorResponses(JSON_CODEC);

	private final KiwiJwtService jwt;
	private final AuthService authService;
	private final long ttlSeconds;
	private final InMemoryRateLimiter rateLimiter;

	public LoginHandler(final KiwiJwtService jwt, final AuthService authService) {
		this(jwt, authService, Long.parseLong(System.getenv().getOrDefault("JWT_TTL_SECONDS", "3600")));
	}

	public LoginHandler(final KiwiJwtService jwt, final AuthService authService, final long ttlSeconds) {
		this(jwt, authService, ttlSeconds, new InMemoryRateLimiter(10, 300));
	}

	public LoginHandler(final KiwiJwtService jwt, final AuthService authService, final long ttlSeconds,
			final InMemoryRateLimiter rateLimiter) {
		super(JSON_CODEC);
		this.jwt = Objects.requireNonNull(jwt);
		this.authService = Objects.requireNonNull(authService);
		this.ttlSeconds = ttlSeconds;
		this.rateLimiter = Objects.requireNonNull(rateLimiter);
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

		// Rate limit: verificar antes de cualquier procesamiento
		final var key = clientIp(jx.request());
		if (!rateLimiter.tryAcquire(key)) {
			return rejectRateLimit(jx, key);
		}

		// 1) Intenta Basic Auth
		final var authz = jx.request().getHeaders().get("authorization");
		if (authz != null && authz.regionMatches(true, 0, "Basic ", 0, "Basic ".length())) {
			final var creds = decodeBasic(authz.substring("Basic ".length()).trim());
			if (creds == null) {
				ERRORS.unauthorized(jx.response(), jx.callback(), "bad_basic_auth");
				return true;
			}
			return authenticateAndMint(jx, creds.user, creds.pass, key);
		}

		// 2) JSON body: {"username":"...","password":"..."}
		final String body;
		try {
			body = BodyReader.read(jx.request(), BodyReader.AUTH_LIMIT);
		} catch (final Exception e) {
			ERRORS.badRequest(jx.response(), jx.callback(), "cannot_read_body");
			return true;
		}
		if (body == null) {
			ERRORS.error(jx.response(), jx.callback(), 413, "payload_too_large", "body_too_large",
					"request body exceeds maximum allowed size");
			return true;
		}

		if (body.isBlank()) {
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
		final var passNode = json.get("password");
		final var pass = passNode != null && passNode.isTextual() ? passNode.asText().toCharArray() : null;

		if (user == null || pass == null) {
			ERRORS.unauthorized(jx.response(), jx.callback(), "missing_credentials");
			return true;
		}

		return authenticateAndMint(jx, user, pass, key);
	}

	private boolean authenticateAndMint(final JettyHttpExchange x, final String username, final char[] password,
			final String rateLimitKey) {

		final var result = authService.authenticate(username, password);

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

		// Autenticación exitosa: resetear el contador de la IP
		rateLimiter.reset(rateLimitKey);

		final var subject = result.userId().toString();
		final var roles = result.roles();
		final var token = jwt.mint(subject, roles, ttlSeconds);

		RESPONSES.ok(x.response(), x.callback(),
				Map.of("token_type", "Bearer", "access_token", token, "expires_in", ttlSeconds));
		return true;
	}

	private boolean rejectRateLimit(final JettyHttpExchange x, final String key) {
		x.response().getHeaders().add("Retry-After", String.valueOf(rateLimiter.retryAfterSeconds(key)));
		ERRORS.error(x.response(), x.callback(), 429, "too_many_requests", "rate_limit_exceeded",
				"too many authentication attempts, try again later");
		return true;
	}

	private static String clientIp(final org.eclipse.jetty.server.Request request) {
		final var addr = request.getConnectionMetaData().getRemoteSocketAddress();
		if (addr instanceof final InetSocketAddress isa) {
			return isa.getAddress().getHostAddress();
		}
		return "unknown";
	}

	private static String text(final JsonNode node, final String field) {
		final var v = node.get(field);
		return v != null && v.isTextual() ? v.asText() : null;
	}

	private record BasicCreds(String user, char[] pass) {
	}

	private static BasicCreds decodeBasic(final String base64Part) {
		try {
			final var bytes = Base64.getDecoder().decode(base64Part);
			int idx = -1;
			for (int i = 0; i < bytes.length; i++) {
				if (bytes[i] == (byte) ':') {
					idx = i;
					break;
				}
			}
			if (idx <= 0) {
				Arrays.fill(bytes, (byte) 0);
				return null;
			}
			final var user = new String(bytes, 0, idx, StandardCharsets.UTF_8);
			final var charBuf = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes, idx + 1, bytes.length - idx - 1));
			final var pass = new char[charBuf.remaining()];
			charBuf.get(pass);
			Arrays.fill(bytes, (byte) 0);
			return new BasicCreds(user, pass);
		} catch (final Exception e) {
			return null;
		}
	}

	private static JettyHttpExchange asJetty(final dev.rafex.ether.http.core.HttpExchange x) {
		return (JettyHttpExchange) x;
	}
}
