package dev.rafex.kiwi.db;

import java.net.URI;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import dev.rafex.kiwi.logging.Log;

public final class Db {

	private static final HikariDataSource DS = create();
	
	private Db() {
    }

	private static HikariDataSource create() {
		final var cfg = new HikariConfig();

		final var dbUrl = System.getenv("DB_URL");
		final var dbUser = System.getenv("DB_USER");
		final var dbPassword = System.getenv("DB_PASSWORD");

		if (dbUrl == null || dbUrl.isBlank()) {
			throw new IllegalStateException("DB_URL environment variable is not set or is empty");
		}

		cfg.setJdbcUrl(dbUrl);

		if (dbUser != null && !dbUser.isBlank()) {
			cfg.setUsername(dbUser);
		}
		if (dbPassword != null && !dbPassword.isBlank()) {
			cfg.setPassword(dbPassword);
		}

		// Pool settings configurable via env, with sensible defaults
		cfg.setMaximumPoolSize(parseIntEnv("DB_MAX_POOL_SIZE", 6));
		cfg.setMinimumIdle(parseIntEnv("DB_MIN_IDLE", 2));
		cfg.setConnectionTimeout(parseLongEnv("DB_CONNECTION_TIMEOUT_MS", 30000L));
		cfg.setIdleTimeout(parseLongEnv("DB_IDLE_TIMEOUT_MS", 600000L));
		cfg.setMaxLifetime(parseLongEnv("DB_MAX_LIFETIME_MS", 1800000L));

		cfg.setPoolName("kiwi-pool");

		// Log a masked/summary form of the DB URL to avoid leaking sensitive data
		Log.info(Db.class, "Database connected: " + maskJdbcUrl(dbUrl));

		return new HikariDataSource(cfg);
	}

	public static DataSource dataSource() {
		return DS;
	}

	private static int parseIntEnv(final String name, final int def) {
		final var v = System.getenv(name);
		if (v == null || v.isBlank()) {
			return def;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (final NumberFormatException e) {
			Log.warn(Db.class, "Invalid integer for " + name + " ('" + v + "'), using default " + def);
			return def;
		}
	}

	private static long parseLongEnv(final String name, final long def) {
		final var v = System.getenv(name);
		if (v == null || v.isBlank()) {
			return def;
		}
		try {
			return Long.parseLong(v.trim());
		} catch (final NumberFormatException e) {
			Log.warn(Db.class, "Invalid long for " + name + " ('" + v + "'), using default " + def);
			return def;
		}
	}

	private static String maskJdbcUrl(final String dbUrl) {
		if (dbUrl == null || dbUrl.isBlank()) {
			return "<not-set>";
		}
		try {
			final var stripped = dbUrl.startsWith("jdbc:") ? dbUrl.substring(5) : dbUrl;
			final var uri = new URI(stripped);
			final var host = uri.getHost();
			final var port = uri.getPort();
			final var path = uri.getPath();
			final var sb = new StringBuilder();
			if (host != null) {
				sb.append(host);
			}
			if (port != -1) {
				sb.append(":").append(port);
			}
			if (path != null && !path.isBlank()) {
				sb.append(path);
			}
			final var out = sb.toString();
			return out.isEmpty() ? "<masked>" : out;
		} catch (final Exception e) {
			return "<masked>";
		}
	}

	
}
