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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HealthHandler extends NonBlockingResourceHandler {

	@Override
	protected String basePath() {
		return "/health";
	}

	@Override
	protected List<Route> routes() {
		return List.of(Route.of("/", Set.of("GET")));
	}

	@Override
	public boolean get(final HttpExchange x) {
		final var body = Map.of("status", "UP", "timestamp", Instant.now().toString());
		HttpUtil.ok(x.response(), x.callback(), body);
		return true;
	}

	@Override
	public Set<String> supportedMethods() {
		return Set.of("GET");
	}

}
