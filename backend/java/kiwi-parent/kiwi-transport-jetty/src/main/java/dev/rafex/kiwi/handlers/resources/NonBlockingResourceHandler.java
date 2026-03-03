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
import dev.rafex.kiwi.logging.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

public abstract class NonBlockingResourceHandler extends Handler.Abstract.NonBlocking implements HttpResource {

	protected abstract String basePath();

	protected List<Route> routes() {
		return List.of(Route.of("/", supportedMethods()));
	}

	@Override
	public boolean handle(final Request request, final Response response, final Callback callback) {
		final var path = request.getHttpURI().getPath();
		if (!matchesBasePath(path)) {
			return false;
		}

		final var relPath = normalizeRelPath(path);
		final var match = matchRoute(relPath);
		if (match.isEmpty()) {
			HttpUtil.notFound(response, callback);
			return true;
		}

		final var routeMatch = match.get();
		final var x = new HttpExchange(request, response, callback, routeMatch.pathParams(), parseQueryMap(request),
				routeMatch.route().allowedMethods());

		final var method = request.getMethod().toUpperCase();
		if (!routeMatch.route().allows(method) && !"OPTIONS".equals(method)) {
			x.methodNotAllowed();
			return true;
		}

		try {
			return dispatch(method, x);
		} catch (final IllegalArgumentException e) {
			HttpUtil.badRequest(response, callback, e.getMessage());
			return true;
		} catch (final Exception e) {
			Log.error(getClass(), "Error handling resource", e);
			HttpUtil.internalServerError(response, callback, "internal_error");
			return true;
		}
	}

	private boolean dispatch(final String method, final HttpExchange x) throws Exception {
		return switch (method) {
			case "GET" -> get(x);
			case "POST" -> post(x);
			case "PUT" -> put(x);
			case "DELETE" -> delete(x);
			case "PATCH" -> patch(x);
			case "OPTIONS" -> options(x);
			default -> {
				x.methodNotAllowed();
				yield true;
			}
		};
	}

	private Optional<RouteMatch> matchRoute(final String relPath) {
		for (final var route : routes()) {
			final var match = route.match(relPath);
			if (match.isPresent()) {
				return Optional.of(new RouteMatch(route, match.get()));
			}
		}
		return Optional.empty();
	}

	private String normalizeRelPath(final String absolutePath) {
		final var base = basePath();
		if (absolutePath.length() == base.length()) {
			return "/";
		}
		final var rel = absolutePath.substring(base.length());
		return rel.isEmpty() ? "/" : rel;
	}

	private boolean matchesBasePath(final String path) {
		final var base = basePath();
		if ("/".equals(base)) {
			return path != null && path.startsWith("/");
		}
		if (base.equals(path)) {
			return true;
		}
		return path.startsWith(base + "/");
	}

	private static Map<String, List<String>> parseQueryMap(final Request request) {
		final MultiMap<String> params = new MultiMap<>();
		final var rawQuery = request.getHttpURI().getQuery();
		if (rawQuery != null && !rawQuery.isEmpty()) {
			UrlEncoded.decodeTo(rawQuery, params, StandardCharsets.UTF_8);
		}

		final var out = new LinkedHashMap<String, List<String>>();
		for (final var key : params.keySet()) {
			final var values = params.getValues(key);
			out.put(key, values == null ? List.of() : List.copyOf(values));
		}
		return out;
	}

	protected static String queryParam(final HttpExchange x, final String key) {
		final var value = x.queryFirst(key);
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}

	public record Route(String pattern, Set<String> allowedMethods) {

		public Route {
			allowedMethods = normalizeMethods(allowedMethods);
		}

		public static Route of(final String pattern, final Set<String> methods) {
			return new Route(pattern, methods);
		}

		public boolean allows(final String method) {
			return allowedMethods.contains(method.toUpperCase());
		}

		public Optional<Map<String, String>> match(final String relPath) {
			if ("/**".equals(pattern)) {
				return Optional.of(Map.of());
			}
			final var patternSeg = split(pattern);
			final var pathSeg = split(relPath);
			if (patternSeg.size() != pathSeg.size()) {
				return Optional.empty();
			}

			final var params = new LinkedHashMap<String, String>();
			for (int i = 0; i < patternSeg.size(); i++) {
				final var expected = patternSeg.get(i);
				final var actual = pathSeg.get(i);

				if (expected.startsWith("{") && expected.endsWith("}")) {
					final var key = expected.substring(1, expected.length() - 1);
					params.put(key, actual);
					continue;
				}
				if (!expected.equals(actual)) {
					return Optional.empty();
				}
			}
			return Optional.of(params);
		}

		private static List<String> split(final String path) {
			final var cleaned = path.startsWith("/") ? path.substring(1) : path;
			if (cleaned.isEmpty()) {
				return List.of();
			}
			return Arrays.asList(cleaned.split("/"));
		}

		private static Set<String> normalizeMethods(final Set<String> methods) {
			final var out = new LinkedHashSet<String>();
			if (methods != null) {
				for (final var method : methods) {
					if (method != null && !method.isBlank()) {
						out.add(method.trim().toUpperCase());
					}
				}
			}
			return Set.copyOf(out);
		}
	}

	private record RouteMatch(Route route, Map<String, String> pathParams) {
	}

}
