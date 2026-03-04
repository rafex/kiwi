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
package dev.rafex.kiwi.server;

import dev.rafex.kiwi.bootstrap.KiwiContainer;
import dev.rafex.kiwi.handlers.JwtAuthHandler;
import dev.rafex.kiwi.json.JsonUtil;
import dev.rafex.kiwi.security.JwtService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.PathMappingsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public final class KiwiServerFactory {

	private KiwiServerFactory() {
	}

	public static KiwiServerRunner create(final KiwiContainer container, final ServerConfig config,
			final List<KiwiModule> modules) {
		Objects.requireNonNull(container, "container");
		Objects.requireNonNull(config, "config");

		final var pool = new QueuedThreadPool();
		pool.setMaxThreads(config.maxThreads());
		pool.setMinThreads(config.minThreads());
		pool.setIdleTimeout(config.idleTimeoutMs());
		pool.setName(config.threadPoolName());

		final var server = new Server(pool);
		final var connector = new ServerConnector(server);
		connector.setPort(config.port());
		server.addConnector(connector);

		final var jwt = new JwtService(JsonUtil.MAPPER, config.jwtIssuer(), config.jwtAudience(), config.jwtSecret());
		final var context = new ModuleContext(container, config, jwt);

		final var routeRegistry = new RouteRegistry();
		final var authPolicyRegistry = new AuthPolicyRegistry();
		final var middlewareRegistry = new MiddlewareRegistry();

		for (final var module : modules == null ? List.<KiwiModule>of() : modules) {
			module.registerRoutes(routeRegistry, context);
			module.registerAuthPolicies(authPolicyRegistry, context);
			module.registerMiddlewares(middlewareRegistry, context);
		}

		final var routesHandler = buildRoutes(routeRegistry.routes());
		final var withAuth = applyAuthPolicies(routesHandler, jwt, authPolicyRegistry.policies());
		final var appHandler = applyMiddlewares(withAuth, middlewareRegistry.middlewares());

		server.setHandler(appHandler);
		return new KiwiServerRunner(server);
	}

	private static PathMappingsHandler buildRoutes(final List<HttpResourceRegistration> registrations) {
		final var routes = new PathMappingsHandler();
		for (final var registration : registrations) {
			routes.addMapping(PathSpec.from(registration.pathSpec()), registration.handler());
		}
		return routes;
	}

	private static Handler applyAuthPolicies(final Handler delegate, final JwtService jwt,
			final List<AuthPolicy> policies) {
		if (policies == null || policies.isEmpty()) {
			return delegate;
		}
		return new JwtAuthHandler(delegate, jwt).authPolicies(policies);
	}

	private static Handler applyMiddlewares(final Handler delegate, final List<Middleware> middlewares) {
		var current = delegate;
		final var stack = middlewares == null ? List.<Middleware>of() : new ArrayList<>(middlewares);
		for (int i = stack.size() - 1; i >= 0; i--) {
			current = stack.get(i).wrap(current);
		}
		return current;
	}

}
