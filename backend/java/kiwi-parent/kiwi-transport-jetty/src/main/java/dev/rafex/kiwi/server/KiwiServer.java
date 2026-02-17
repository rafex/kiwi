package dev.rafex.kiwi.server;

import java.util.logging.Logger;

import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.PathMappingsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import dev.rafex.kiwi.bootstrap.KiwiContainer;
import dev.rafex.kiwi.handlers.GlowrootNamingHandler;
import dev.rafex.kiwi.handlers.HealthHandler;
import dev.rafex.kiwi.handlers.HelloHandler;
import dev.rafex.kiwi.handlers.JwtAuthHandler;
import dev.rafex.kiwi.handlers.LocationHandler;
import dev.rafex.kiwi.handlers.LoginHandler;
import dev.rafex.kiwi.handlers.NotFoundHandler;
import dev.rafex.kiwi.handlers.ObjectHandler;
import dev.rafex.kiwi.json.JsonUtil;
import dev.rafex.kiwi.security.JwtService;

public final class KiwiServer {

    private static final Logger LOG = Logger.getLogger(KiwiServer.class.getName());

    private KiwiServer() {
    }

    public static void start(final KiwiContainer container) throws Exception {
        final var port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        final var pool = new QueuedThreadPool();
        pool.setMaxThreads(Math.max(Runtime.getRuntime().availableProcessors() * 2, 16));
        pool.setMinThreads(4);
        pool.setIdleTimeout(30_000);
        pool.setName("kiwi-http");

        final var server = new Server(pool);

        final var connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        final var objectService = container.objectService();
        final var locationService = container.locationService();

        // JWT config
        final var jwt = new JwtService(JsonUtil.MAPPER, System.getenv().getOrDefault("JWT_ISS", "dev.rafex.kiwi"),
                System.getenv().getOrDefault("JWT_AUD", "kiwi-backend"),
                System.getenv().getOrDefault("JWT_SECRET", "CHANGE_ME_NOW_32+chars_secret"));

        final var routes = new PathMappingsHandler();
        routes.addMapping(PathSpec.from("/hello"), new HelloHandler());
        routes.addMapping(PathSpec.from("/health"), new HealthHandler());
        routes.addMapping(PathSpec.from("/auth/login"), new LoginHandler(jwt));
        routes.addMapping(PathSpec.from("/objects/*"), new ObjectHandler(objectService));
        routes.addMapping(PathSpec.from("/locations/*"), new LocationHandler(locationService));
        routes.addMapping(PathSpec.from("/*"), new NotFoundHandler()); // fallback (según versión/impl)

        // Wrapper: /hello público, /objects y /locations protegidos (ajústalo a tu
        // gusto)
        final var auth = new JwtAuthHandler(routes, jwt).publicPath("GET", "/auth/login").publicPath("GET", "/hello")
                .publicPath("GET", "/health").protectedPrefix("/objects/*").protectedPrefix("/locations/*");

        server.setHandler(new GlowrootNamingHandler(auth));

        LOG.info("Starting Kiwi backend on port " + port);
        server.start();
        server.join();
    }
}
