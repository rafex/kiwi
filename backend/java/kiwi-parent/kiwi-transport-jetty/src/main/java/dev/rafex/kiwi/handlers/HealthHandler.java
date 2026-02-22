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

import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.json.JsonUtil;

import java.time.Instant;
import java.util.Map;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public class HealthHandler extends Handler.Abstract.NonBlocking {

	@Override
	public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
		if (!"/health".equals(request.getHttpURI().getPath())) {
			return false;
		}
		if (request.getMethod() == null || !HttpMethod.GET.is(request.getMethod())) {
			response.setStatus(405);
			return true;
		}

		final var body = Map.of("status", "UP", "timestamp", Instant.now().toString());

		HttpUtil.ok(response, callback, JsonUtil.toJson(body));
		return true;
	}

}
