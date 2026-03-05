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
import dev.rafex.ether.http.jetty12.JettyApiErrorResponses;
import dev.rafex.kiwi.logging.Log;

import java.util.List;
import java.util.Set;

public final class NotFoundHandler extends NonBlockingResourceHandler {

	private static final JsonCodec JSON_CODEC = JsonUtils.codec();
	private static final JettyApiErrorResponses ERRORS = new JettyApiErrorResponses(JSON_CODEC);

	public NotFoundHandler() {
		super(JSON_CODEC);
	}

	@Override
	protected String basePath() {
		return "/";
	}

	@Override
	protected List<Route> routes() {
		return List.of(Route.of("/**", Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")));
	}

	@Override
	public Set<String> supportedMethods() {
		return Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
	}

	@Override
	public boolean get(final dev.rafex.ether.http.core.HttpExchange x) {
		return notFound(asJetty(x));
	}

	@Override
	public boolean post(final dev.rafex.ether.http.core.HttpExchange x) {
		return notFound(asJetty(x));
	}

	@Override
	public boolean put(final dev.rafex.ether.http.core.HttpExchange x) {
		return notFound(asJetty(x));
	}

	@Override
	public boolean patch(final dev.rafex.ether.http.core.HttpExchange x) {
		return notFound(asJetty(x));
	}

	@Override
	public boolean delete(final dev.rafex.ether.http.core.HttpExchange x) {
		return notFound(asJetty(x));
	}

	@Override
	public boolean options(final dev.rafex.ether.http.core.HttpExchange x) {
		return notFound(asJetty(x));
	}

	private boolean notFound(final JettyHttpExchange x) {
		Log.debug(getClass(), "No handler found for path: {}", x.path());
		ERRORS.notFound(x.response(), x.callback(), x.path());
		return true;
	}

	private static JettyHttpExchange asJetty(final dev.rafex.ether.http.core.HttpExchange x) {
		return (JettyHttpExchange) x;
	}
}
