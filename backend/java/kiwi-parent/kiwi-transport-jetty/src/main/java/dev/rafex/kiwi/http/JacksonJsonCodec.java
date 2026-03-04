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
package dev.rafex.kiwi.http;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JacksonJsonCodec implements JsonCodec {

	private final ObjectMapper mapper;

	public JacksonJsonCodec(final ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public static JacksonJsonCodec defaultCodec() {
		final var mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return new JacksonJsonCodec(mapper);
	}

	@Override
	public String toJson(final Object value) {
		try {
			return mapper.writeValueAsString(value);
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException("Error serializing object to JSON", e);
		}
	}

	@Override
	public <T> T readValue(final InputStream input, final Class<T> type) throws IOException {
		return mapper.readValue(input, type);
	}

	@Override
	public JsonNode readTree(final String content) throws IOException {
		return mapper.readTree(content);
	}

	@Override
	public JsonNode readTree(final InputStream input) throws IOException {
		return mapper.readTree(input);
	}

}

