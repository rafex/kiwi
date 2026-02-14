package dev.rafex.kiwi;

import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.rafex.kiwi.server.KiwiServer;

public class App {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    public static void main(final String[] args) throws Exception {

        configureLogging(args);

        LOG.info("Starting Kiwi backend...");
        KiwiServer.start();
    }

    private static void configureLogging(final String[] args) {

        var levelStr = System.getenv().getOrDefault("LOG_LEVEL", "INFO");

        // Permite override por argumento --log=DEBUG
        for (final String arg : args) {
            if (arg.startsWith("--log=")) {
                levelStr = arg.substring("--log=".length());
            }
        }

        final var level = mapLevel(levelStr);

        final var root = Logger.getLogger("");
        root.setLevel(level);

        for (final Handler handler : root.getHandlers()) {
            handler.setLevel(level);
        }

        LOG.info("Log level set to " + level.getName());
    }

    private static Level mapLevel(final String levelStr) {
        return switch (levelStr.toUpperCase(Locale.ROOT)) {
        case "DEBUG" -> Level.FINE;
        case "WARN" -> Level.WARNING;
        case "ERROR" -> Level.SEVERE;
        case "INFO" -> Level.INFO;
        default -> Level.INFO;
        };
    }
}