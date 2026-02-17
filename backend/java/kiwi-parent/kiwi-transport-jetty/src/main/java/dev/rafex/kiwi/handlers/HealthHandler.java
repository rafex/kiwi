package dev.rafex.kiwi.handlers;

import java.time.Instant;
import java.util.Map;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.json.JsonUtil;

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
