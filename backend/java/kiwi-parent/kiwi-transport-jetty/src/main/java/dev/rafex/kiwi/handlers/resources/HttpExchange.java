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
package dev.rafex.kiwi.handlers.resources;

import dev.rafex.kiwi.http.HttpUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public final class HttpExchange {

	private final Request request;
	private final Response response;
	private final Callback callback;
	private final Map<String, String> pathParams;
	private final Map<String, List<String>> queryParams;
	private final Set<String> allowedMethods;

	HttpExchange(final Request request, final Response response, final Callback callback,
			final Map<String, String> pathParams, final Map<String, List<String>> queryParams,
			final Set<String> allowedMethods) {
		this.request = request;
		this.response = response;
		this.callback = callback;
		this.pathParams = pathParams;
		this.queryParams = queryParams;
		this.allowedMethods = normalizeMethods(allowedMethods);
	}

	public Request request() {
		return request;
	}

	public Response response() {
		return response;
	}

	public Callback callback() {
		return callback;
	}

	public String path() {
		return request.getHttpURI().getPath();
	}

	public String method() {
		return request.getMethod();
	}

	public String pathParam(final String name) {
		return pathParams.get(name);
	}

	public String queryFirst(final String name) {
		final var values = queryParams.get(name);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.get(0);
	}

	public List<String> queryAll(final String name) {
		return queryParams.getOrDefault(name, List.of());
	}

	public Map<String, String> pathParams() {
		return Collections.unmodifiableMap(pathParams);
	}

	public Map<String, List<String>> queryParams() {
		return Collections.unmodifiableMap(queryParams);
	}

	public Set<String> allowedMethods() {
		return Collections.unmodifiableSet(allowedMethods);
	}

	public void json(final int status, final Object body) {
		HttpUtil.json(response, callback, status, body);
	}

	public void text(final int status, final String body) {
		response.setStatus(status);
		response.getHeaders().put("content-type", "text/plain; charset=utf-8");
		final var payload = body == null ? "" : body;
		final var bytes = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		response.write(true, java.nio.ByteBuffer.wrap(bytes), callback);
	}

	public void methodNotAllowed() {
		response.getHeaders().put("Allow", String.join(", ", allowedMethods));
		HttpUtil.error(response, callback, HttpStatus.METHOD_NOT_ALLOWED_405, "method_not_allowed", null,
				"method not allowed", path());
	}

	public void options() {
		response.getHeaders().put("Allow", String.join(", ", allowedMethods));
		response.setStatus(HttpStatus.NO_CONTENT_204);
		callback.succeeded();
	}

	private static Set<String> normalizeMethods(final Set<String> methods) {
		final var out = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		if (methods != null) {
			for (final var m : methods) {
				if (m != null && !m.isBlank()) {
					out.add(m.trim().toUpperCase());
				}
			}
		}
		out.add("OPTIONS");
		return out;
	}

}
