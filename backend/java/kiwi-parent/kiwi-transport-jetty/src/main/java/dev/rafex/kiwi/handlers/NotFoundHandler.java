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

import java.util.List;
import java.util.Set;

public final class NotFoundHandler extends NonBlockingResourceHandler {

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
	public boolean get(final HttpExchange x) {
		return notFound(x);
	}

	@Override
	public boolean post(final HttpExchange x) {
		return notFound(x);
	}

	@Override
	public boolean put(final HttpExchange x) {
		return notFound(x);
	}

	@Override
	public boolean patch(final HttpExchange x) {
		return notFound(x);
	}

	@Override
	public boolean delete(final HttpExchange x) {
		return notFound(x);
	}

	@Override
	public boolean options(final HttpExchange x) {
		return notFound(x);
	}

	private boolean notFound(final HttpExchange x) {
		Log.debug(getClass(), "No handler found for path: {}", x.path());
		HttpUtil.notFound(x.response(), x.callback(), x.path());
		return true;
	}
}
