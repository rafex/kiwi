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

import org.eclipse.jetty.server.Request;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utilidad para leer el cuerpo de una request HTTP con un límite de tamaño.
 *
 * <p>Protege contra ataques de tipo "large payload DoS" leyendo como máximo
 * {@code maxBytes} bytes del stream de entrada. Si el cuerpo supera el límite,
 * devuelve {@code null} para que el handler responda con HTTP 413.
 */
final class BodyReader {

	/** 8 KB — suficiente para credenciales y payloads administrativos pequeños. */
	static final int AUTH_LIMIT = 8 * 1024;

	/** 256 KB — permite objetos con metadata moderada. */
	static final int OBJECT_LIMIT = 256 * 1024;

	private BodyReader() {
	}

	/**
	 * Lee el cuerpo de la request como String UTF-8, con límite de tamaño.
	 *
	 * @param request  request de Jetty
	 * @param maxBytes número máximo de bytes permitidos
	 * @return el cuerpo como String, o {@code null} si supera {@code maxBytes}
	 * @throws IOException si ocurre un error de I/O leyendo el stream
	 */
	static String read(final Request request, final int maxBytes) throws IOException {
		try (final var in = Request.asInputStream(request)) {
			final var bytes = in.readNBytes(maxBytes + 1);
			if (bytes.length > maxBytes) {
				return null;
			}
			return new String(bytes, StandardCharsets.UTF_8);
		}
	}
}
