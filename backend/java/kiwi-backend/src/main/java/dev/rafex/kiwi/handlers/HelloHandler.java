package dev.rafex.kiwi.handlers;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.services.HelloServices;

public class HelloHandler extends Handler.Abstract.NonBlocking {

    private final HelloServices services = new HelloServices();

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

        Log.debug(getClass(), "GET /hello");

        response.setStatus(200);
        response.getHeaders().put("content-type", "application/json; charset=utf-8");

        HttpUtil.ok(response, callback, services.sayHello());
        return true;
    }

}
