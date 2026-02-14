package dev.rafex.kiwi.db;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class Db {

	private static final Logger LOG = LoggerFactory.getLogger(Db.class);
	private static final HikariDataSource DS = create();

	private static HikariDataSource create() {
		final var cfg = new HikariConfig();

		cfg.setJdbcUrl(System.getenv("DB_URL"));
		cfg.setUsername(System.getenv("DB_USER"));
		cfg.setPassword(System.getenv("DB_PASSWORD"));

		cfg.setMaximumPoolSize(6);
		cfg.setMinimumIdle(2);
		cfg.setPoolName("kiwi-pool");

		LOG.info("Database connected: {}", System.getenv("DB_URL"));

		return new HikariDataSource(cfg);
	}

	public static DataSource dataSource() {
		return DS;
	}

	private Db() {
	}
}
