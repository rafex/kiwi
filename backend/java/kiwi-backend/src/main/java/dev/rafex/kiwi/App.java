package dev.rafex.kiwi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.rafex.kiwi.server.KiwiServer;

public class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(final String[] args) throws Exception {

        LOG.info("Starting Kiwi backend...");
        KiwiServer.start();
    }
}