package dev.rafex.kiwi.server;

import java.util.logging.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public final class KiwiServer {

	private static final Logger LOG = Logger.getLogger(KiwiServer.class.getName());

	private KiwiServer() {
	}

	public static void start() throws Exception {
		int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

		QueuedThreadPool pool = new QueuedThreadPool();
		pool.setName("kiwi-http");

		Server server = new Server(pool);

		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);

		server.setHandler(new RouterHandler());

		LOG.info("Starting Kiwi backend on port " + port);
		server.start();
		server.join();
	}
}
