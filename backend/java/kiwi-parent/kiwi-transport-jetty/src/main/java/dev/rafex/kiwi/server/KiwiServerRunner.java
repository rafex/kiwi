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

import org.eclipse.jetty.server.Server;

public final class KiwiServerRunner {

	private final Server server;

	KiwiServerRunner(final Server server) {
		this.server = server;
	}

	public void start() throws Exception {
		server.start();
	}

	public void await() throws Exception {
		server.join();
	}

	public void stop() throws Exception {
		server.stop();
	}

	public Server server() {
		return server;
	}

}
