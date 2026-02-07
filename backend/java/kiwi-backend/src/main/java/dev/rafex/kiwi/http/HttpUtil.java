package dev.rafex.kiwi.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public final class HttpUtil {

	private HttpUtil() {
	}

	public static void json(Response response, Callback callback, int status, String jsonBody) {
		response.setStatus(status);
		response.getHeaders().put("content-type", "application/json; charset=utf-8");

		byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
		response.write(true, ByteBuffer.wrap(bytes), callback);
	}

	public static void ok(Response response, Callback callback, String jsonBody) {
		json(response, callback, 200, jsonBody);
	}

	public static void notFound(Response response, Callback callback) {
		json(response, callback, 404, "{\"error\":\"not_found\"}");
	}

	public static void badRequest(Response response, Callback callback, String message) {
		json(response, callback, 400, "{\"error\":\"bad_request\",\"message\":\"" + escape(message) + "\"}");
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
