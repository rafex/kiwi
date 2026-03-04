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
import dev.rafex.kiwi.security.JwtService;
import dev.rafex.kiwi.services.AppClientAuthService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import com.fasterxml.jackson.databind.JsonNode;

public final class TokenHandler extends NonBlockingResourceHandler {

	private final JwtService jwt;
	private final AppClientAuthService authService;
	private final long ttlSeconds;

	public TokenHandler(final JwtService jwt, final AppClientAuthService authService) {
		this(jwt, authService, Long.parseLong(System.getenv().getOrDefault("JWT_APP_TTL_SECONDS", "1800")));
	}

	public TokenHandler(final JwtService jwt, final AppClientAuthService authService, final long ttlSeconds) {
		this.jwt = Objects.requireNonNull(jwt);
		this.authService = Objects.requireNonNull(authService);
		this.ttlSeconds = ttlSeconds;
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
	public boolean post(final HttpExchange x) throws Exception {
		final var authz = x.request().getHeaders().get("authorization");
		if (authz != null && authz.regionMatches(true, 0, "Basic ", 0, "Basic ".length())) {
			final var creds = decodeBasic(authz.substring("Basic ".length()).trim());
			if (creds == null) {
				HttpUtil.unauthorized(x.response(), x.callback(), "invalid_client");
				return true;
			}
			return authenticateAndMint(x, creds.clientId(), creds.clientSecret(), "client_credentials");
		}

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

		final var contentType = x.request().getHeaders().get("content-type");
		if (contentType != null && contentType.toLowerCase().contains("application/json")) {
			final JsonNode json;
			try {
				json = HttpUtil.jsonCodec().readTree(body);
			} catch (final Exception e) {
				HttpUtil.badRequest(x.response(), x.callback(), "invalid_json");
				return true;
			}

			return authenticateAndMint(x, text(json, "client_id"), text(json, "client_secret"),
					text(json, "grant_type"));
		}

		final var form = new MultiMap<String>();
		UrlEncoded.decodeTo(body, form, StandardCharsets.UTF_8);
		return authenticateAndMint(x, value(form, "client_id"), value(form, "client_secret"),
				value(form, "grant_type"));
	}

	private boolean authenticateAndMint(final HttpExchange x, final String clientId,
			final String clientSecret, final String grantType) throws Exception {
		if (grantType == null || !"client_credentials".equals(grantType)) {
			HttpUtil.badRequest(x.response(), x.callback(), "unsupported_grant_type");
			return true;
		}

		if (clientId == null || clientSecret == null) {
			HttpUtil.unauthorized(x.response(), x.callback(), "invalid_client");
			return true;
		}

		final var result = authService.authenticate(clientId, clientSecret.toCharArray());
		if (!result.ok()) {
			final var code = result.code() != null ? result.code() : "invalid_client";
			if ("client_disabled".equals(code)) {
				HttpUtil.forbidden(x.response(), x.callback(), "client_disabled");
			} else if ("invalid_client".equals(code)) {
				HttpUtil.unauthorized(x.response(), x.callback(), "invalid_client");
			} else {
				HttpUtil.unauthorized(x.response(), x.callback(), code);
			}
			return true;
		}

		final var token = jwt.mintApp("app:" + result.clientId(), result.clientId(), result.roles(), ttlSeconds);
		HttpUtil.ok(x.response(), x.callback(),
				Map.of("token_type", "Bearer", "access_token", token, "expires_in", ttlSeconds, "grant_type",
						"client_credentials"));
		return true;
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

	private record BasicCreds(String clientId, String clientSecret) {
	}

	private static BasicCreds decodeBasic(final String base64Part) {
		try {
			final var decoded = new String(Base64.getDecoder().decode(base64Part), StandardCharsets.UTF_8);
			final var idx = decoded.indexOf(':');
			if (idx <= 0) {
				return null;
			}
			return new BasicCreds(decoded.substring(0, idx), decoded.substring(idx + 1));
		} catch (final Exception e) {
			return null;
		}
	}
}
