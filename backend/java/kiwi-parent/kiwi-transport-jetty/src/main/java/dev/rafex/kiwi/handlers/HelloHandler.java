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
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.services.HelloService;
import dev.rafex.kiwi.services.impl.HelloServiceImpl;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public class HelloHandler extends Handler.Abstract.NonBlocking {

	private final HelloService services = new HelloServiceImpl();

	// @Instrumentation.Transaction(transactionType = "Web", transactionName =
	// "{{0.method}} {{0.httpURI.path}}", traceHeadline = "{{0.method}}
	// {{0.httpURI.path}}", timer = "jetty-handler")
	@Override
	public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

		Log.debug(getClass(), "GET /hello");

		HttpUtil.ok(response, callback, JsonUtil.toJson(services.sayHello()));
		return true;
	}

}
