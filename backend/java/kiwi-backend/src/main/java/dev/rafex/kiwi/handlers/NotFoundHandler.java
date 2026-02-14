package dev.rafex.kiwi.handlers;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.logging.Log;

public final class NotFoundHandler extends Handler.Abstract {

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
        Log.debug(getClass(), "No handler found for path: {}", request.getHttpURI().getPath());
        HttpUtil.notFound(response, callback, request.getHttpURI().getPath()); // reutiliza el body constante de HttpUtil
        return true;
    }
}