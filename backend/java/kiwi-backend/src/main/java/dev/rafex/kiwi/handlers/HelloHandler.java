package dev.rafex.kiwi.handlers;

import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.logging.Log;

public class HelloHandler {

	private static final Logger LOG = Logger.getLogger(HelloHandler.class.getName());

	public boolean handle(Request request, Response response, Callback callback) throws Exception {

		Log.debug(getClass(), "GET /hello");

		response.setStatus(200);
		response.getHeaders().put("content-type", "application/json; charset=utf-8");

		HttpUtil.ok(response, callback, "{\"name\":\"kiwi\",\"status\":\"ok\"}");

		return true;
	}
}
