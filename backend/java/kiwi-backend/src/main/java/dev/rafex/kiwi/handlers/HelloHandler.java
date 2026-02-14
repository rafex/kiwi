package dev.rafex.kiwi.handlers;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.services.HelloServices;

public class HelloHandler extends Handler.Abstract.NonBlocking {

    private static final Logger LOG = LoggerFactory.getLogger(HelloHandler.class);
    private final HelloServices services;

    public HelloHandler(final HelloServices services) {
        this.services = services;
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

        LOG.debug("GET /hello");

        HttpUtil.ok(response, callback, services.sayHello());
        return true;
    }

}
