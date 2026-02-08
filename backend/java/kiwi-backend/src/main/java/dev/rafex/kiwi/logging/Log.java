package dev.rafex.kiwi.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class Log {

	private Log() {
	}

	public static Logger get(final Class<?> clazz) {
		return Logger.getLogger(clazz.getName());
	}

	public static void info(final Class<?> clazz, final String msg) {
		get(clazz).info(msg);
	}

	public static void warn(final Class<?> clazz, final String msg) {
		get(clazz).warning(msg);
	}

	public static void error(final Class<?> clazz, final String msg, final Throwable t) {
		final var log = get(clazz);
		log.log(Level.SEVERE, msg, t);
	}

	public static void debug(final Class<?> clazz, final String msg) {
		get(clazz).fine(msg);
	}
}
