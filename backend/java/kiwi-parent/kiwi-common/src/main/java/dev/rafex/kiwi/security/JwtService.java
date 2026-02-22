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
package dev.rafex.kiwi.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JwtService {

	public record AuthContext(String sub, long exp, String iss, String aud, List<String> roles) {
	}

	public record VerifyResult(boolean ok, AuthContext ctx, String code) {
		public static VerifyResult ok(final AuthContext ctx) {
			return new VerifyResult(true, ctx, null);
		}

		public static VerifyResult bad(final String code) {
			return new VerifyResult(false, null, code);
		}
	}

	private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
	};
	private static final Base64.Encoder B64U_ENC = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder B64U_DEC = Base64.getUrlDecoder();

	private final ObjectMapper mapper;
	private final String iss;
	private final String aud;
	private final byte[] secret;

	public JwtService(final ObjectMapper mapper, final String iss, final String aud, final String secret) {
		this.mapper = Objects.requireNonNull(mapper);
		this.iss = Objects.requireNonNull(iss);
		this.aud = Objects.requireNonNull(aud);

		if (secret == null || secret.length() < 32) {
			throw new IllegalArgumentException("JWT_SECRET demasiado corto (usa >= 32 chars).");
		}
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
	}

	/** Backwards-compatible: token sin roles. */
	public String mint(final String sub, final long ttlSeconds) {
		return mint(sub, List.of(), ttlSeconds);
	}

	/** Token con roles (claim: "roles": ["admin","writer"]) */
	public String mint(final String sub, final Collection<String> roles, final long ttlSeconds) {
		final var now = Instant.now().getEpochSecond();
		final var exp = now + ttlSeconds;

		final var headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

		// roles JSON (sin depender de JsonUtil)
		final var rolesJson = rolesToJsonArray(roles);

		final var payloadJson = "{" + "\"sub\":\"" + escapeJson(sub) + "\"," + "\"iss\":\"" + escapeJson(iss) + "\","
				+ "\"aud\":\"" + escapeJson(aud) + "\"," + "\"iat\":" + now + "," + "\"exp\":" + exp + ","
				+ "\"roles\":" + rolesJson + "}";

		final var h = b64u(headerJson.getBytes(StandardCharsets.UTF_8));
		final var p = b64u(payloadJson.getBytes(StandardCharsets.UTF_8));
		final var signingInput = h + "." + p;

		final var sig = hmacSha256(secret, signingInput.getBytes(StandardCharsets.UTF_8));
		return signingInput + "." + b64u(sig);
	}

	public VerifyResult verify(final String token, final long nowEpochSeconds) {
		try {
			final var parts = token.split("\\.");
			if (parts.length != 3) {
				return VerifyResult.bad("bad_format");
			}

			final var h = parts[0];
			final var p = parts[1];
			final var s = parts[2];

			final var headerJson = new String(b64uDec(h), StandardCharsets.UTF_8);
			final Map<String, Object> header = mapper.readValue(headerJson, MAP);
			final var alg = header.get("alg");
			if (!"HS256".equals(alg)) {
				return VerifyResult.bad("unsupported_alg");
			}

			final var signingInput = h + "." + p;
			final var expected = hmacSha256(secret, signingInput.getBytes(StandardCharsets.UTF_8));
			final var provided = b64uDec(s);
			if (!MessageDigest.isEqual(expected, provided)) {
				return VerifyResult.bad("bad_signature");
			}

			final var payloadJson = new String(b64uDec(p), StandardCharsets.UTF_8);
			final Map<String, Object> payload = mapper.readValue(payloadJson, MAP);

			final var sub = asString(payload.get("sub"));
			final var issGot = asString(payload.get("iss"));
			final var audGot = asString(payload.get("aud"));
			final var exp = asLong(payload.get("exp"));
			final var roles = asStringList(payload.get("roles"));

			if (sub == null || sub.isBlank()) {
				return VerifyResult.bad("missing_sub");
			}
			if (exp == null) {
				return VerifyResult.bad("missing_exp");
			}
			if (nowEpochSeconds >= exp) {
				return VerifyResult.bad("token_expired");
			}
			if (!Objects.equals(iss, issGot)) {
				return VerifyResult.bad("bad_iss");
			}
			if (!Objects.equals(aud, audGot)) {
				return VerifyResult.bad("bad_aud");
			}

			return VerifyResult.ok(new AuthContext(sub, exp, issGot, audGot, roles));
		} catch (final Exception e) {
			return VerifyResult.bad("verify_exception");
		}
	}

	private static byte[] hmacSha256(final byte[] secret, final byte[] data) {
		try {
			final var mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret, "HmacSHA256"));
			return mac.doFinal(data);
		} catch (final Exception e) {
			throw new IllegalStateException("HMAC failed", e);
		}
	}

	private static String b64u(final byte[] bytes) {
		return B64U_ENC.encodeToString(bytes);
	}

	private static byte[] b64uDec(final String s) {
		return B64U_DEC.decode(s);
	}

	private static String asString(final Object o) {
		return o == null ? null : String.valueOf(o);
	}

	private static Long asLong(final Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof final Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o));
		} catch (final NumberFormatException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static List<String> asStringList(final Object o) {
		if (o == null) {
			return List.of();
		}

		// JSON array -> jackson lo deja como List<?> normalmente
		if (o instanceof final List<?> list) {
			return list.stream().filter(Objects::nonNull).map(String::valueOf).filter(s -> !s.isBlank()).toList();
		}

		// si viene como "admin" (string) por alguna razón
		final var s = String.valueOf(o);
		if (s.isBlank()) {
			return List.of();
		}
		return List.of(s);
	}

	private static String rolesToJsonArray(final Collection<String> roles) {
		if (roles == null || roles.isEmpty()) {
			return "[]";
		}
		final var sb = new StringBuilder();
		sb.append('[');
		var first = true;
		for (final var r : roles) {
			if (r == null) {
				continue;
			}
			final var v = r.trim();
			if (v.isEmpty()) {
				continue;
			}
			if (!first) {
				sb.append(',');
			}
			sb.append('"').append(escapeJson(v)).append('"');
			first = false;
		}
		sb.append(']');
		return sb.toString();
	}

	// sin depender de JsonUtil aquí
	private static String escapeJson(final String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}