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

import dev.rafex.ether.jwt.DefaultTokenIssuer;
import dev.rafex.ether.jwt.DefaultTokenVerifier;
import dev.rafex.ether.jwt.JwtConfig;
import dev.rafex.ether.jwt.KeyProvider;
import dev.rafex.ether.jwt.TokenClaims;
import dev.rafex.ether.jwt.TokenIssuer;
import dev.rafex.ether.jwt.TokenSpec;
import dev.rafex.ether.jwt.TokenType;
import dev.rafex.ether.jwt.TokenVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Kiwi adapter over ether-jwt preserving the auth contract expected by handlers.
 */
public final class KiwiJwtService {

	public record AuthContext(String sub, long exp, String iss, String aud, List<String> roles, String tokenType,
			String clientId) {
	}

	public record VerifyResult(boolean ok, AuthContext ctx, String code) {
		public static VerifyResult ok(final AuthContext ctx) {
			return new VerifyResult(true, ctx, null);
		}

		public static VerifyResult bad(final String code) {
			return new VerifyResult(false, null, code);
		}
	}

	private final String iss;
	private final String aud;
	private final TokenIssuer issuer;
	private final TokenVerifier verifier;

	public KiwiJwtService(final String iss, final String aud, final String secret) {
		this.iss = Objects.requireNonNull(iss, "iss");
		this.aud = Objects.requireNonNull(aud, "aud");
		if (secret == null || secret.length() < 32) {
			throw new IllegalArgumentException("JWT_SECRET demasiado corto (usa >= 32 chars).");
		}

		final var keyProvider = KeyProvider.hmac(secret);
		final var config = JwtConfig.builder(keyProvider)
				.expectedIssuer(iss)
				.expectedAudience(aud)
				.build();
		this.issuer = new DefaultTokenIssuer(config);
		this.verifier = new DefaultTokenVerifier(config);
	}

	public String mint(final String sub, final Collection<String> roles, final long ttlSeconds) {
		final var now = Instant.now();
		final var spec = TokenSpec.builder()
				.subject(sub)
				.issuer(iss)
				.audience(aud)
				.issuedAt(now)
				.ttl(Duration.ofSeconds(ttlSeconds))
				.roles(roles == null ? new String[0] : roles.toArray(String[]::new))
				.tokenType(TokenType.USER)
				.build();
		return issuer.issue(spec);
	}

	public String mintApp(final String sub, final String clientId, final Collection<String> roles, final long ttlSeconds) {
		final var now = Instant.now();
		final var spec = TokenSpec.builder()
				.subject(sub)
				.issuer(iss)
				.audience(aud)
				.issuedAt(now)
				.ttl(Duration.ofSeconds(ttlSeconds))
				.roles(roles == null ? new String[0] : roles.toArray(String[]::new))
				.tokenType(TokenType.APP)
				.clientId(clientId)
				.build();
		return issuer.issue(spec);
	}

	public VerifyResult verify(final String token, final long nowEpochSeconds) {
		final var result = verifier.verify(token, Instant.ofEpochSecond(nowEpochSeconds));
		if (!result.ok()) {
			return VerifyResult.bad(result.code());
		}
		final var claims = result.claims().orElse(null);
		if (claims == null) {
			return VerifyResult.bad("verify_exception");
		}
		return VerifyResult.ok(toContext(claims));
	}

	private static AuthContext toContext(final TokenClaims claims) {
		final var tokenType = claims.tokenType() == null ? "user" : claims.tokenType().claimValue();
		final var audience = claims.audience().isEmpty() ? null : claims.audience().get(0);
		return new AuthContext(
				claims.subject(),
				claims.expiresAt() == null ? 0L : claims.expiresAt().getEpochSecond(),
				claims.issuer(),
				audience,
				claims.roles(),
				tokenType,
				claims.clientId());
	}
}
