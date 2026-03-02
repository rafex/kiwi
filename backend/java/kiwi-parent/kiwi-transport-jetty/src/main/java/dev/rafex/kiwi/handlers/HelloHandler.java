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

import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.tools.BuildVersion;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

public class HelloHandler extends Handler.Abstract.NonBlocking {

	// @Instrumentation.Transaction(transactionType = "Web", transactionName =
	// "{{0.method}} {{0.httpURI.path}}", traceHeadline = "{{0.method}}
	// {{0.httpURI.path}}", timer = "jetty-handler")
	@Override
	public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

		Log.debug(getClass(), "GET /hello");

		if (!HttpMethod.GET.is(request.getMethod())) {
			response.setStatus(405);
			callback.succeeded();
			return true;
		}

		final var rawQuery = request.getHttpURI() != null ? request.getHttpURI().getQuery() : null;
		final var queryParams = parseQuery(rawQuery);
		final var name = normalize(queryParams.getValue("name"));

		final var message = name == null ? "Hello!!" : "Hello!! " + name;
		HttpUtil.ok(response, callback, Map.of("message", message, "version", BuildVersion.current()));
		return true;
	}

	private static MultiMap<String> parseQuery(final String rawQuery) {
		final var params = new MultiMap<String>();
		if (rawQuery != null && !rawQuery.isBlank()) {
			UrlEncoded.decodeTo(rawQuery, params, StandardCharsets.UTF_8);
		}
		return params;
	}

	private static String normalize(final String value) {
		if (value == null) {
			return null;
		}
		final var trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
