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

import dev.rafex.kiwi.handlers.CreateAppClientHandler;
import dev.rafex.kiwi.handlers.CreateUserHandler;
import dev.rafex.kiwi.handlers.GlowrootNamingHandler;
import dev.rafex.kiwi.handlers.HealthHandler;
import dev.rafex.kiwi.handlers.HelloHandler;
import dev.rafex.kiwi.handlers.LocationHandler;
import dev.rafex.kiwi.handlers.LoginHandler;
import dev.rafex.kiwi.handlers.NotFoundHandler;
import dev.rafex.kiwi.handlers.ObjectHandler;
import dev.rafex.kiwi.handlers.TokenHandler;

public final class DefaultKiwiModule implements KiwiModule {

	@Override
	public void registerRoutes(final RouteRegistry routes, final ModuleContext context) {
		final var container = context.container();
		final var jwt = context.jwtService();

		routes.add("/hello", new HelloHandler());
		routes.add("/health", new HealthHandler());
		routes.add("/auth/login", new LoginHandler(jwt, container.authService()));
		routes.add("/auth/token", new TokenHandler(jwt, container.appClientAuthService()));
		routes.add("/objects/*", new ObjectHandler(container.objectService()));
		routes.add("/locations/*", new LocationHandler(container.locationService()));
		routes.add("/admin/app-clients", new CreateAppClientHandler(container.appClientAuthService()));

		if (context.config().enableUserProvisioning() && context.config().isSandbox()) {
			routes.add("/admin/users", new CreateUserHandler(container.userProvisioningService()));
		}

		routes.add("/*", new NotFoundHandler());
	}

	@Override
	public void registerAuthPolicies(final AuthPolicyRegistry authPolicies, final ModuleContext context) {
		authPolicies.publicPath("POST", "/admin/users");
		authPolicies.publicPath("POST", "/auth/login");
		authPolicies.publicPath("POST", "/auth/token");
		authPolicies.publicPath("GET", "/hello");
		authPolicies.publicPath("GET", "/health");

		authPolicies.protectedPrefix("/objects/*");
		authPolicies.protectedPrefix("/locations/*");
		authPolicies.protectedPrefix("/admin/app-clients");
	}

	@Override
	public void registerMiddlewares(final MiddlewareRegistry middlewares, final ModuleContext context) {
		middlewares.add(GlowrootNamingHandler::new);
	}

}
