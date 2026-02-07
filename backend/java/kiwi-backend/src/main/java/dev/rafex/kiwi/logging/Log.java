package dev.rafex.kiwi.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class Log {

	private Log() {
	}

	public static Logger get(Class<?> clazz) {
		return Logger.getLogger(clazz.getName());
	}

	public static void info(Class<?> clazz, String msg) {
		get(clazz).info(msg);
	}

	public static void warn(Class<?> clazz, String msg) {
		get(clazz).warning(msg);
	}

	public static void error(Class<?> clazz, String msg, Throwable t) {
		Logger log = get(clazz);
		log.log(Level.SEVERE, msg, t);
	}

	public static void debug(Class<?> clazz, String msg) {
		get(clazz).fine(msg);
	}
}
