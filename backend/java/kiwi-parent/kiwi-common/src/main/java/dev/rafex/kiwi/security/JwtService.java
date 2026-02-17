package dev.rafex.kiwi.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JwtService {

    public record AuthContext(String sub, long exp, String iss, String aud) {
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

    public String mint(final String sub, final long ttlSeconds) {
        final var now = Instant.now().getEpochSecond();
        final var exp = now + ttlSeconds;

        final var headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        final var payloadJson = "{\"sub\":\"" + escapeJson(sub) + "\"," + "\"iss\":\"" + escapeJson(iss) + "\","
                + "\"aud\":\"" + escapeJson(aud) + "\"," + "\"iat\":" + now + "," + "\"exp\":" + exp + "}";

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

            return VerifyResult.ok(new AuthContext(sub, exp, issGot, audGot));
        } catch (Exception e) {
            return VerifyResult.bad("verify_exception");
        }
    }

    private static byte[] hmacSha256(final byte[] secret, final byte[] data) {
        try {
            final var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
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
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // sin depender de JsonUtil aquí
    private static String escapeJson(final String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}