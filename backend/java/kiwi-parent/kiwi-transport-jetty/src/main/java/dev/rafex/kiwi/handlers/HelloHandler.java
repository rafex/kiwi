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

import dev.rafex.ether.http.jetty12.JettyHttpExchange;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;
import dev.rafex.ether.json.JsonUtils;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.JettyApiResponses;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.tools.BuildVersion;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class HelloHandler extends NonBlockingResourceHandler {

	private static final JsonCodec JSON_CODEC = JsonUtils.codec();
	private static final JettyApiResponses RESPONSES = new JettyApiResponses(JSON_CODEC);

	public HelloHandler() {
		super(JSON_CODEC);
	}

	@Override
	protected String basePath() {
		return "/hello";
	}

	@Override
	protected List<Route> routes() {
		return List.of(Route.of("/", Set.of("GET")));
	}

	@Override
	public boolean get(final dev.rafex.ether.http.core.HttpExchange x) {
		final var jx = asJetty(x);
		Log.debug(getClass(), "GET /hello");
		final var name = normalize(queryParam(jx, "name"));
		final var message = name == null ? "Hello!!" : "Hello!! " + name;
		RESPONSES.ok(jx.response(), jx.callback(), Map.of("message", message, "version", BuildVersion.current()));
		return true;
	}

	@Override
	public Set<String> supportedMethods() {
		return Set.of("GET");
	}

	private static String normalize(final String value) {
		if (value == null) {
			return null;
		}
		final var trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static JettyHttpExchange asJetty(final dev.rafex.ether.http.core.HttpExchange x) {
		return (JettyHttpExchange) x;
	}

}
