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

import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasherPBKDF2 {

	// Debe coincidir con el largo de hash que guardas (ej: 32 bytes = 256 bits)
	// Si ya tienes hashes con otro tamaño, ajusta.
	private final int derivedKeyBytes;

	public PasswordHasherPBKDF2(final int derivedKeyBytes) {
		if (derivedKeyBytes < 16) {
			throw new IllegalArgumentException("derivedKeyBytes demasiado pequeño");
		}
		this.derivedKeyBytes = derivedKeyBytes;
	}

	public boolean verify(final char[] password, final byte[] salt, final int iterations, final byte[] expectedHash) {
		if (password == null || salt == null || expectedHash == null || (iterations <= 0)) {
			return false;
		}

		final var dk = derive(password, salt, iterations, expectedHash.length);
		try {
			return MessageDigest.isEqual(dk, expectedHash);
		} finally {
			Arrays.fill(dk, (byte) 0);
		}
	}

	/**
	 * Deriva PBKDF2WithHmacSHA256. Nota: Java usa "bits" en PBEKeySpec.
	 */
	private static byte[] derive(final char[] password, final byte[] salt, final int iterations,
			final int outLenBytes) {
		try {
			final var spec = new PBEKeySpec(password, salt, iterations, outLenBytes * 8);
			final var skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			return skf.generateSecret(spec).getEncoded();
		} catch (final Exception e) {
			throw new IllegalStateException("PBKDF2 derivation failed", e);
		}
	}

	/**
	 * Helper si en algún momento quieres generar hashes nuevos. (No lo uso aún en
	 * authenticate, pero es útil para registrar usuarios.)
	 */
	public HashResult hash(final char[] password, final byte[] salt, final int iterations) {
		final var dk = derive(password, salt, iterations, derivedKeyBytes);
		return new HashResult(dk, salt, iterations);
	}

	public record HashResult(byte[] hash, byte[] salt, int iterations) {
	}
}