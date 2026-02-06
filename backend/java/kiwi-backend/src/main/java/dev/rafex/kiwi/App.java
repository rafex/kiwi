package dev.rafex.kiwi;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class App {

	private static final Logger LOG = Logger.getLogger(App.class.getName());

	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

		// ThreadPool (igual que en la doc)
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setName("server");

		Server server = new Server(threadPool);

		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);

		server.setHandler(new Handler.Abstract() {
			@Override
			public boolean handle(Request request, Response response, Callback callback) throws Exception {
				try {
					String method = request.getMethod();
					String path = request.getHttpURI().getPath();

					if ("GET".equals(method) && "/hello".equals(path)) {
						LOG.info("GET /hello");

						response.setStatus(200);
						response.getHeaders().put("content-type", "application/json; charset=utf-8");

						byte[] body = """
								{"name":"kiwi","description":"BD para objetos y ubicaciones","status":"ok"}
								""".trim().getBytes(StandardCharsets.UTF_8);

						// Jetty 12 Response write: escribe bytes y notifica via callback
						response.write(true, java.nio.ByteBuffer.wrap(body), callback);
						return true;
					}

					response.setStatus(404);
					response.getHeaders().put("content-type", "application/json; charset=utf-8");
					byte[] body = "{\"error\":\"not_found\"}".getBytes(StandardCharsets.UTF_8);
					response.write(true, java.nio.ByteBuffer.wrap(body), callback);
					return true;

				} catch (Throwable t) {
					response.setStatus(500);
					callback.failed(t);
					return true;
				}
			}

		});

		LOG.info("Starting Kiwi backend on port " + port);
		server.start();
		server.join();
	}

}