package dev.rafex.kiwi.server;

import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.kiwi.handlers.HelloHandler;

public class RouterHandler extends Handler.Abstract {

	private final HelloHandler helloHandler = new HelloHandler();

	@Override
	public boolean handle(Request request, Response response, Callback callback) {
		try {
			String method = request.getMethod();
			String path = request.getHttpURI().getPath();

			if ("GET".equals(method) && "/hello".equals(path)) {
				return helloHandler.handle(request, response, callback);
			}

			response.setStatus(404);
			response.getHeaders().put("content-type", "application/json; charset=utf-8");
			byte[] body = "{\"error\":\"not_found\"}".getBytes(StandardCharsets.UTF_8);
			response.write(true, java.nio.ByteBuffer.wrap(body), callback);
			callback.succeeded();
			return true;
		} catch (Throwable t) {
			response.setStatus(500);
			callback.failed(t);
			return true;
		}
	}
}
