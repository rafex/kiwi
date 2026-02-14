package dev.rafex.kiwi.server;

import java.util.logging.Logger;

import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.PathMappingsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import dev.rafex.kiwi.handlers.HelloHandler;
import dev.rafex.kiwi.handlers.LocationHandler;
import dev.rafex.kiwi.handlers.NotFoundHandler;
import dev.rafex.kiwi.handlers.ObjectHandler;

public final class KiwiServer {

    private static final Logger LOG = Logger.getLogger(KiwiServer.class.getName());

    private KiwiServer() {
    }

    public static void start() throws Exception {
        final var port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        final var pool = new QueuedThreadPool();
        pool.setName("kiwi-http");

        final var server = new Server(pool);

        final var connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        final var routes = new PathMappingsHandler();
        routes.addMapping(PathSpec.from("/hello"), new HelloHandler());
        routes.addMapping(PathSpec.from("/objects/*"), new ObjectHandler());
        routes.addMapping(PathSpec.from("/locations/*"), new LocationHandler());
        routes.addMapping(PathSpec.from("/*"), new NotFoundHandler()); // fallback (según versión/impl)

        server.setHandler(routes);

        LOG.info("Starting Kiwi backend on port " + port);
        server.start();
        server.join();
    }
}
