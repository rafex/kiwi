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

import dev.rafex.kiwi.handlers.resources.HttpExchange;
import dev.rafex.kiwi.handlers.resources.NonBlockingResourceHandler;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.tools.BuildVersion;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class HelloHandler extends NonBlockingResourceHandler {

	@Override
	protected String basePath() {
		return "/hello";
	}

	@Override
	protected List<Route> routes() {
		return List.of(Route.of("/", Set.of("GET")));
	}

	@Override
	public boolean get(final HttpExchange x) {
		Log.debug(getClass(), "GET /hello");
		final var name = normalize(queryParam(x, "name"));
		final var message = name == null ? "Hello!!" : "Hello!! " + name;
		HttpUtil.ok(x.response(), x.callback(), Map.of("message", message, "version", BuildVersion.current()));
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

}
