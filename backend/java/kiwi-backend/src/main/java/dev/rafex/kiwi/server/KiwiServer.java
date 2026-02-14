package dev.rafex.kiwi.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.PathMappingsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import dev.rafex.kiwi.db.Db;
import dev.rafex.kiwi.db.LocationRepository;
import dev.rafex.kiwi.db.ObjectRepository;
import dev.rafex.kiwi.handlers.HelloHandler;
import dev.rafex.kiwi.handlers.LocationHandler;
import dev.rafex.kiwi.handlers.NotFoundHandler;
import dev.rafex.kiwi.handlers.ObjectHandler;
import dev.rafex.kiwi.services.HelloServices;
import dev.rafex.kiwi.services.LocationServices;
import dev.rafex.kiwi.services.ObjectServices;

public final class KiwiServer {

    private static final Logger LOG = LoggerFactory.getLogger(KiwiServer.class);

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

        final var ds = Db.dataSource();

        // Repositories
        final var objectRepo = new ObjectRepository(ds);
        final var locationRepo = new LocationRepository(ds);

        // Services
        final var helloServices = new HelloServices();
        final var objectServices = new ObjectServices(objectRepo);
        final var locationServices = new LocationServices(locationRepo);

        final var routes = new PathMappingsHandler();
        routes.addMapping(PathSpec.from("/hello"), new HelloHandler(helloServices));
        routes.addMapping(PathSpec.from("/objects/*"), new ObjectHandler(objectServices));
        routes.addMapping(PathSpec.from("/locations/*"), new LocationHandler(locationServices));
        routes.addMapping(PathSpec.from("/*"), new NotFoundHandler()); // fallback (según versión/impl)

        server.setHandler(routes);

        LOG.info("Starting Kiwi backend on port {}", port);
        server.start();
        server.join();
    }
}
