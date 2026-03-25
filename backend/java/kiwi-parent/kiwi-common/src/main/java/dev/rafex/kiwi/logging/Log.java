/*
 * Copyright 2026 Raúl Eduardo González Argote
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.rafex.kiwi.logging;

import dev.rafex.ether.logging.core.config.LoggingConfigurator;
import dev.rafex.ether.logging.core.format.LogMessageFormatter;
import dev.rafex.ether.logging.core.level.LogLevels;
import dev.rafex.ether.logging.core.logger.EtherLog;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.slf4j.MDC;

public final class Log {

	private Log() {
	}

	private static Logger get(final Class<?> clazz) {
		return EtherLog.get(clazz);
	}

	public static Level parseLevel(final String rawLevel, final Level defaultLevel) {
		return LogLevels.parse(rawLevel, defaultLevel);
	}

	public static boolean isSupportedLevel(final String rawLevel) {
		return LogLevels.isSupported(rawLevel);
	}

	public static Logger configureRootLogger(final Level level) {
		return LoggingConfigurator.configureRootLogger(level);
	}

	/* ===================== INFO ===================== */

	public static void info(final Class<?> clazz, final String msg) {
		EtherLog.info(clazz, msg);
	}

	public static void info(final Class<?> clazz, final String msg, final Object... args) {
		EtherLog.info(clazz, msg, args);
	}

	public static void info(final Class<?> clazz, final Throwable t, final String msg, final Object... args) {
		EtherLog.info(clazz, t, msg, args);
	}

	/* ===================== WARN ===================== */

	public static void warn(final Class<?> clazz, final String msg) {
		EtherLog.warn(clazz, msg);
	}

	public static void warn(final Class<?> clazz, final String msg, final Object... args) {
		EtherLog.warn(clazz, msg, args);
	}

	public static void warn(final Class<?> clazz, final Throwable t, final String msg, final Object... args) {
		EtherLog.warn(clazz, t, msg, args);
	}

	/* ===================== ERROR ===================== */

	public static void error(final Class<?> clazz, final String msg) {
		EtherLog.error(clazz, msg);
	}

	public static void error(final Class<?> clazz, final String msg, final Object... args) {
		EtherLog.error(clazz, msg, args);
	}

	public static void error(final Class<?> clazz, final String msg, final Throwable t) {
		EtherLog.error(clazz, msg, t);
	}

	/**
	 * Firma clave para tu KiwiError: Log.error(getClass(), cause, "KiwiError [{}]:
	 * {}", code, message);
	 */
	public static void error(final Class<?> clazz, final Throwable t, final String msg, final Object... args) {
		EtherLog.error(clazz, t, msg, args);
	}

	/* ===================== DEBUG ===================== */

	public static void debug(final Class<?> clazz, final String msg) {
		EtherLog.debug(clazz, msg);
	}

	public static void debug(final Class<?> clazz, final String msg, final Object... args) {
		EtherLog.debug(clazz, msg, args);
	}

	public static void debug(final Class<?> clazz, final Throwable t, final String msg, final Object... args) {
		EtherLog.debug(clazz, t, msg, args);
	}

	/* ===================== INTERNAL FORMAT ===================== */

	private static String format(final String message, final Object... args) {
		return LogMessageFormatter.format(message, args);
	}

	/*
	 * ===================== MDC (Mapped Diagnostic Context) =====================
	 */

	/**
	 * MDC key for request ID.
	 */
	public static final String MDC_REQUEST_ID = "requestId";

	/**
	 * MDC key for user ID.
	 */
	public static final String MDC_USER_ID = "userId";

	/**
	 * Put a key-value pair into the MDC.
	 */
	public static void put(final String key, final String value) {
		MDC.put(key, value);
	}

	/**
	 * Get the value for a key from the MDC.
	 */
	public static String get(final String key) {
		return MDC.get(key);
	}

	/**
	 * Remove a key from the MDC.
	 */
	public static void remove(final String key) {
		MDC.remove(key);
	}

	/**
	 * Clear all entries from the MDC.
	 */
	public static void clear() {
		MDC.clear();
	}

	/**
	 * Execute a Runnable with the given MDC context. The context is removed after
	 * execution.
	 */
	public static void withContext(final java.util.Map<String, String> context, final Runnable task) {
		try {
			context.forEach(MDC::put);
			task.run();
		} finally {
			context.keySet().forEach(MDC::remove);
		}
	}

	/**
	 * Execute a Runnable with requestId and userId in MDC context.
	 */
	public static void withRequestContext(final String requestId, final String userId, final Runnable task) {
		try {
			if (requestId != null) {
				MDC.put(MDC_REQUEST_ID, requestId);
			}
			if (userId != null) {
				MDC.put(MDC_USER_ID, userId);
			}
			task.run();
		} finally {
			if (requestId != null) {
				MDC.remove(MDC_REQUEST_ID);
			}
			if (userId != null) {
				MDC.remove(MDC_USER_ID);
			}
		}
	}
}
