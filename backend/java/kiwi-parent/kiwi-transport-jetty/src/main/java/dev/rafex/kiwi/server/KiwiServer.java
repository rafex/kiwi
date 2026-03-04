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
import java.util.logging.Logger;

import dev.rafex.ether.http.jetty12.JettyRouteRegistry;
import dev.rafex.ether.http.jetty12.JettyServerConfig;
import dev.rafex.ether.http.jetty12.JettyServerFactory;

public final class KiwiServer {

	private static final Logger LOG = Logger.getLogger(KiwiServer.class.getName());

	private KiwiServer() {
	}

	public static void start(final KiwiContainer container) throws Exception {
		start(container, ServerConfig.fromEnv(), List.of(new DefaultKiwiModule()));
	}

	public static void start(final KiwiContainer container, final ServerConfig config, final List<KiwiModule> modules)
			throws Exception {
		final var runner = createRunner(container, config, modules);
		LOG.info("Starting Kiwi backend on port " + config.port());
		runner.start();
		runner.await();
	}

	private static dev.rafex.ether.http.jetty12.JettyServerRunner createRunner(final KiwiContainer container,
			final ServerConfig config, final List<KiwiModule> modules) {
		Objects.requireNonNull(container, "container");
		Objects.requireNonNull(config, "config");

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

		final var etherRoutes = new JettyRouteRegistry();
		for (final var route : routeRegistry.routes()) {
			etherRoutes.add(route.pathSpec(), route.handler());
		}

		final var etherMiddlewares = new ArrayList<dev.rafex.ether.http.jetty12.JettyMiddleware>();
		for (final var middleware : middlewareRegistry.middlewares()) {
			etherMiddlewares.add(middleware::wrap);
		}
		if (!authPolicyRegistry.policies().isEmpty()) {
			etherMiddlewares.add(next -> new JwtAuthHandler(next, jwt).authPolicies(authPolicyRegistry.policies()));
		}

		final var etherConfig = new JettyServerConfig(config.port(), config.maxThreads(), config.minThreads(),
				config.idleTimeoutMs(), config.threadPoolName(), config.environment());
		final var jsonCodec = new dev.rafex.ether.json.JacksonJsonCodec(JsonUtil.MAPPER);
		return JettyServerFactory.create(etherConfig, etherRoutes, jsonCodec, null, List.of(), etherMiddlewares);
	}
}
