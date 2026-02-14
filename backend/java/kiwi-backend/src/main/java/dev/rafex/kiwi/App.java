package dev.rafex.kiwi;

import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.rafex.kiwi.server.KiwiServer;

public class App {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    public static void main(final String[] args) throws Exception {

        // Consistencia global (opcional pero recomendable si quieres resultados
        // deterministas)
        Locale.setDefault(Locale.ROOT);
        // Si quieres UTC siempre:
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        configureLogging(args);

        LOG.info("Starting Kiwi backend...");
        KiwiServer.start();
    }

    private static void configureLogging(final String[] args) {

        // 1) Fuente: env var LOG_LEVEL (default INFO)
        String levelStr = System.getenv().getOrDefault("LOG_LEVEL", "INFO");

        // 2) Override por CLI: --log=DEBUG
        for (String arg : args) {
            if (arg != null && arg.startsWith("--log=")) {
                levelStr = arg.substring("--log=".length());
                break;
            }
        }

        final Level level = parseAllowedLevel(levelStr);

        // Config root logger + handlers
        final Logger root = Logger.getLogger("");
        root.setLevel(level);
        for (Handler h : root.getHandlers()) {
            h.setLevel(level);
        }

        LOG.info("Log level set to " + level.getName());
    }

    private static Level parseAllowedLevel(final String levelStr) {
        if (levelStr == null || levelStr.isBlank()) {
            return Level.INFO;
        }

        final String v = levelStr.trim().toUpperCase(Locale.ROOT);

        return switch (v) {
            case "DEBUG" -> Level.FINE;
            case "INFO"  -> Level.INFO;
            case "WARN"  -> Level.WARNING;
            case "ERROR" -> Level.SEVERE;
            default -> {
                // validación fuerte: si te pasan un valor inválido, lo fuerzas a INFO y lo avisas
                System.err.println("[kiwi] Invalid LOG_LEVEL/--log value: '" + levelStr
                        + "'. Allowed: DEBUG, INFO, WARN, ERROR. Defaulting to INFO.");
                yield Level.INFO;
            }
        };
    }
}