package dev.rafex.kiwi.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class Log {

    private Log() {
    }

    private static final ClassValue<Logger> LOGGERS = new ClassValue<>() {
        @Override
        protected Logger computeValue(final Class<?> clazz) {
            return Logger.getLogger(clazz.getName());
        }
    };

    private static Logger get(final Class<?> clazz) {
        return LOGGERS.get(clazz);
    }

    /* ===================== INFO ===================== */

    public static void info(final Class<?> clazz, final String msg) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.INFO)) {
            return;
        }
        log.info(msg);
    }

    public static void info(final Class<?> clazz, final String msg, final Object... args) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.INFO)) {
            return;
        }
        log.info(format(msg, args));
    }

    public static void info(final Class<?> clazz, final Throwable t, final String msg, final Object... args) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.INFO)) {
            return;
        }
        log.log(Level.INFO, format(msg, args), t);
    }

    /* ===================== WARN ===================== */

    public static void warn(final Class<?> clazz, final String msg) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.WARNING)) {
            return;
        }
        log.warning(msg);
    }

    public static void warn(final Class<?> clazz, final String msg, final Object... args) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.WARNING)) {
            return;
        }
        log.warning(format(msg, args));
    }

    public static void warn(final Class<?> clazz, final Throwable t, final String msg, final Object... args) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.WARNING)) {
            return;
        }
        log.log(Level.WARNING, format(msg, args), t);
    }

    /* ===================== ERROR ===================== */

    public static void error(final Class<?> clazz, final String msg) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.SEVERE)) {
            return;
        }
        log.severe(msg);
    }

    public static void error(final Class<?> clazz, final String msg, final Object... args) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.SEVERE)) {
            return;
        }
        log.severe(format(msg, args));
    }

    public static void error(final Class<?> clazz, final String msg, final Throwable t) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.SEVERE)) {
            return;
        }
        log.log(Level.SEVERE, msg, t);
    }

    /**
     * Firma clave para tu KiwiError: Log.error(getClass(), cause, "KiwiError [{}]:
     * {}", code, message);
     */
    public static void error(final Class<?> clazz, final Throwable t, final String msg, final Object... args) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.SEVERE)) {
            return;
        }
        log.log(Level.SEVERE, format(msg, args), t);
    }

    /* ===================== DEBUG ===================== */

    public static void debug(final Class<?> clazz, final String msg) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.FINE)) {
            return;
        }
        log.fine(msg);
    }

    public static void debug(final Class<?> clazz, final String msg, final Object... args) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.FINE)) {
            return;
        }
        log.fine(format(msg, args));
    }

    public static void debug(final Class<?> clazz, final Throwable t, final String msg, final Object... args) {
        final var log = get(clazz);
        if (!log.isLoggable(Level.FINE)) {
            return;
        }
        log.log(Level.FINE, format(msg, args), t);
    }

    /* ===================== INTERNAL FORMAT ===================== */

    private static String format(final String message, final Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        final var sb = new StringBuilder(message.length() + 64);
        var argIdx = 0;
        var start = 0;
        int idx;
        while ((idx = message.indexOf("{}", start)) >= 0 && argIdx < args.length) {
            sb.append(message, start, idx);
            sb.append(args[argIdx] == null ? "null" : args[argIdx].toString());
            start = idx + 2;
            argIdx++;
        }
        sb.append(message, start, message.length());
        return sb.toString();
    }
}