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
import dev.rafex.kiwi.services.AppClientAuthService;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import com.fasterxml.jackson.databind.JsonNode;

public final class TokenHandler extends NonBlockingResourceHandler {

	private static final JsonCodec JSON_CODEC = JsonUtils.codec();
	private static final JettyApiResponses RESPONSES = new JettyApiResponses(JSON_CODEC);
	private static final JettyApiErrorResponses ERRORS = new JettyApiErrorResponses(JSON_CODEC);

	private final KiwiJwtService jwt;
	private final AppClientAuthService authService;
	private final long ttlSeconds;
	private final InMemoryRateLimiter rateLimiter;

	public TokenHandler(final KiwiJwtService jwt, final AppClientAuthService authService) {
		this(jwt, authService, Long.parseLong(System.getenv().getOrDefault("JWT_APP_TTL_SECONDS", "1800")));
	}

	public TokenHandler(final KiwiJwtService jwt, final AppClientAuthService authService, final long ttlSeconds) {
		this(jwt, authService, ttlSeconds, new InMemoryRateLimiter(10, 300));
	}

	public TokenHandler(final KiwiJwtService jwt, final AppClientAuthService authService, final long ttlSeconds,
			final InMemoryRateLimiter rateLimiter) {
		super(JSON_CODEC);
		this.jwt = Objects.requireNonNull(jwt);
		this.authService = Objects.requireNonNull(authService);
		this.ttlSeconds = ttlSeconds;
		this.rateLimiter = Objects.requireNonNull(rateLimiter);
	}

	@Override
	protected String basePath() {
		return "/auth/token";
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

		final var authz = jx.request().getHeaders().get("authorization");
		if (authz != null && authz.regionMatches(true, 0, "Basic ", 0, "Basic ".length())) {
			final var creds = decodeBasic(authz.substring("Basic ".length()).trim());
			if (creds == null) {
				ERRORS.unauthorized(jx.response(), jx.callback(), "invalid_client");
				return true;
			}
			return authenticateAndMint(jx, creds.clientId(), creds.clientSecret(), "client_credentials", key);
		}

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
			ERRORS.badRequest(jx.response(), jx.callback(), "missing_body");
			return true;
		}

		final var contentType = jx.request().getHeaders().get("content-type");
		if (contentType != null && contentType.toLowerCase().contains("application/json")) {
			final JsonNode json;
			try {
				json = JSON_CODEC.readTree(body);
			} catch (final Exception e) {
				ERRORS.badRequest(jx.response(), jx.callback(), "invalid_json");
				return true;
			}

			final var secretNode = json.get("client_secret");
			final var secretChars = secretNode != null && secretNode.isTextual() ? secretNode.asText().toCharArray() : null;
			return authenticateAndMint(jx, text(json, "client_id"), secretChars, text(json, "grant_type"), key);
		}

		final var form = new MultiMap<String>();
		UrlEncoded.decodeTo(body, form, StandardCharsets.UTF_8);
		final var secretStr = value(form, "client_secret");
		return authenticateAndMint(jx, value(form, "client_id"), secretStr != null ? secretStr.toCharArray() : null,
				value(form, "grant_type"), key);
	}

	private boolean authenticateAndMint(final JettyHttpExchange x, final String clientId, final char[] clientSecret,
			final String grantType, final String rateLimitKey) {
		if (grantType == null || !"client_credentials".equals(grantType)) {
			ERRORS.badRequest(x.response(), x.callback(), "unsupported_grant_type");
			return true;
		}

		if (clientId == null || clientSecret == null) {
			ERRORS.unauthorized(x.response(), x.callback(), "invalid_client");
			return true;
		}

		final var result = authService.authenticate(clientId, clientSecret);
		if (!result.ok()) {
			final var code = result.code() != null ? result.code() : "invalid_client";
			if ("client_disabled".equals(code)) {
				ERRORS.forbidden(x.response(), x.callback(), "client_disabled");
			} else if ("invalid_client".equals(code)) {
				ERRORS.unauthorized(x.response(), x.callback(), "invalid_client");
			} else {
				ERRORS.unauthorized(x.response(), x.callback(), code);
			}
			return true;
		}

		// Autenticación exitosa: resetear el contador de la IP
		rateLimiter.reset(rateLimitKey);

		final var token = jwt.mintApp("app:" + result.clientId(), result.clientId(), result.roles(), ttlSeconds);
		RESPONSES.ok(x.response(), x.callback(), Map.of("token_type", "Bearer", "access_token", token, "expires_in",
				ttlSeconds, "grant_type", "client_credentials"));
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
		if (node == null) {
			return null;
		}
		final var v = node.get(field);
		return v != null && v.isTextual() ? v.asText() : null;
	}

	private static String value(final MultiMap<String> form, final String field) {
		final var v = form.getValue(field);
		return v == null || v.isBlank() ? null : v;
	}

	private record BasicCreds(String clientId, char[] clientSecret) {
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
			final var clientId = new String(bytes, 0, idx, StandardCharsets.UTF_8);
			final var charBuf = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes, idx + 1, bytes.length - idx - 1));
			final var clientSecret = new char[charBuf.remaining()];
			charBuf.get(clientSecret);
			Arrays.fill(bytes, (byte) 0);
			return new BasicCreds(clientId, clientSecret);
		} catch (final Exception e) {
			return null;
		}
	}

	private static JettyHttpExchange asJetty(final dev.rafex.ether.http.core.HttpExchange x) {
		return (JettyHttpExchange) x;
	}
}
