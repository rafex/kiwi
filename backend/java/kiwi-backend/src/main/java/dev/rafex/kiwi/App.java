package dev.rafex.kiwi;

import java.util.logging.Logger;

import dev.rafex.kiwi.server.KiwiServer;

public class App {

	private static final Logger LOG = Logger.getLogger(App.class.getName());

	public static void main(String[] args) throws Exception {
		LOG.info("Starting Kiwi backend...");
		KiwiServer.start();
	}

}