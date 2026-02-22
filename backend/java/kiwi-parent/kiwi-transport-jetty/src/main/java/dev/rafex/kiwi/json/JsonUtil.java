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
package dev.rafex.kiwi.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonUtil {

	public static final ObjectMapper MAPPER = createMapper();

	private JsonUtil() {
		throw new IllegalStateException("Utility class");
	}

	private static ObjectMapper createMapper() {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return mapper;
	}

	/**
	 * Serializa cualquier objeto a JSON String.
	 */
	public static String toJson(final Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException("Error serializing object to JSON", e);
		}
	}

	/**
	 * Escapa correctamente un String para JSON usando Jackson.
	 */
	public static String escapeJson(final String value) {
		try {
			// Jackson lo serializa con comillas, las removemos
			final var json = MAPPER.writeValueAsString(value);
			return json.substring(1, json.length() - 1);
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException("Error escaping JSON string", e);
		}
	}
}