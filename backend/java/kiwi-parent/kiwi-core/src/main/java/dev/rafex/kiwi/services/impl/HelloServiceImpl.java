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
package dev.rafex.kiwi.services.impl;

import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.services.HelloService;

import java.util.Map;

public class HelloServiceImpl implements HelloService {

	@Override
	public Map<String, String> sayHello(final String name) {

		final var realName = name == null ? "kiwi" : name;

		Log.debug(getClass(), "sayHello called with name={}", realName);

		return Map.of("name", realName, "status", "ok");
	}

	@Override
	public Map<String, String> sayHello() {
		return sayHello(null);
	}
}
