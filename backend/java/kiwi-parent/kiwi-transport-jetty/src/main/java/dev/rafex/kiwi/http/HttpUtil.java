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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.ether.json.JacksonJsonCodec;
import dev.rafex.ether.json.JsonCodec;

public final class HttpUtil {

	private static volatile JsonCodec jsonCodec = JacksonJsonCodec.defaultCodec();

	private HttpUtil() {
	}

	/**
	 * Escribe una respuesta JSON. Si `body` es una cadena se asume JSON ya
	 * formateado, en caso contrario se serializa con el {@link dev.rafex.ether.json.JsonCodec}
	 * configurado.
	 */
	public static void json(final Response response, final Callback callback, final int status, final Object body) {
		response.setStatus(status);
		response.getHeaders().put("content-type", "application/json; charset=utf-8");

		final var jsonBody = body instanceof final String s ? s : jsonCodec.toJson(body);
		final var bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
		response.write(true, ByteBuffer.wrap(bytes), callback);
	}

	public static void json(final Response response, final Callback callback, final int status, final String jsonBody) {
		json(response, callback, status, (Object) jsonBody);
	}

	public static void ok(final Response response, final Callback callback, final Object body) {
		json(response, callback, HttpStatus.OK_200, body);
	}

	public static void ok(final Response response, final Callback callback, final String jsonBody) {
		json(response, callback, HttpStatus.OK_200, (Object) jsonBody);
	}

	public static void ok_noContent(final Response response, final Callback callback) {
		response.setStatus(HttpStatus.NO_CONTENT_204);
		callback.succeeded();
	}

	public static void notFound(final Response response, final Callback callback) {
		error(response, callback, HttpStatus.NOT_FOUND_404, "not_found", null, "resource not found", null);

	}

	public static void notFound(final Response response, final Callback callback, final String path) {
		error(response, callback, HttpStatus.NOT_FOUND_404, "not_found", null, "resource not found", path);

	}

	public static void badRequest(final Response response, final Callback callback, final String message) {
		error(response, callback, HttpStatus.BAD_REQUEST_400, "bad_request", "bad_request", message, null);
	}

	public static void internalServerError(final Response response, final Callback callback, final String message) {
		error(response, callback, HttpStatus.INTERNAL_SERVER_ERROR_500, "internal_server_error", "internal_error",
				message, null);
	}

	public static void unauthorized(final Response response, final Callback callback, final String code) {
		error(response, callback, HttpStatus.UNAUTHORIZED_401, "unauthorized", code, null, null);

	}

	public static void forbidden(final Response response, final Callback callback, final String code) {
		error(response, callback, HttpStatus.FORBIDDEN_403, "forbidden", code, null, null);
	}

	public static void error(final Response response, final Callback callback, final MappedHttpError mapped) {
		if (mapped == null) {
			internalServerError(response, callback, "internal_error");
			return;
		}
		error(response, callback, mapped.status(), mapped.error(), mapped.code(), mapped.message(), null);
	}

	public static void error(final Response response, final Callback callback, final int status, final String error,
			final String code) {
		error(response, callback, status, error, code, null, null);
	}

	public static void error(final Response response, final Callback callback, final int status, final String error,
			final String code, final String message) {
		error(response, callback, status, error, code, message, null);
	}

	public static void error(final Response response, final Callback callback, final int status, final String error,
			final String code, final String message, final String path) {
		final var payload = new LinkedHashMap<String, Object>();
		payload.put("error", error);
		if (code != null && !code.isBlank()) {
			payload.put("code", code);
		}
		if (message != null && !message.isBlank()) {
			payload.put("message", message);
		}
		if (path != null && !path.isBlank()) {
			payload.put("path", path);
		}
		payload.put("timestamp", Instant.now().toString());
		json(response, callback, status, payload);
	}

	public static JsonCodec jsonCodec() {
		return jsonCodec;
	}

	public static void setJsonCodec(final JsonCodec codec) {
		if (codec != null) {
			jsonCodec = codec;
		}
	}

}
