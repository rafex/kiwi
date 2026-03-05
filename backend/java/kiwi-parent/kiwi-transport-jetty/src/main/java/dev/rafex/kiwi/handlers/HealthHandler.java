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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HealthHandler extends NonBlockingResourceHandler {

	private static final JsonCodec JSON_CODEC = JsonUtils.codec();
	private static final JettyApiResponses RESPONSES = new JettyApiResponses(JSON_CODEC);

	public HealthHandler() {
		super(JSON_CODEC);
	}

	@Override
	protected String basePath() {
		return "/health";
	}

	@Override
	protected List<Route> routes() {
		return List.of(Route.of("/", Set.of("GET")));
	}

	@Override
	public boolean get(final dev.rafex.ether.http.core.HttpExchange x) {
		final var jx = asJetty(x);
		final var body = Map.of("status", "UP", "timestamp", Instant.now().toString());
		RESPONSES.ok(jx.response(), jx.callback(), body);
		return true;
	}

	@Override
	public Set<String> supportedMethods() {
		return Set.of("GET");
	}

	private static JettyHttpExchange asJetty(final dev.rafex.ether.http.core.HttpExchange x) {
		return (JettyHttpExchange) x;
	}

}
