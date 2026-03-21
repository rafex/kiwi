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
package dev.rafex.kiwi.handlers;

import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.JettyApiResponses;
import dev.rafex.ether.http.jetty12.JettyHttpExchange;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;
import dev.rafex.ether.json.JsonUtils;
import dev.rafex.ether.observability.core.probe.ProbeAggregator;
import dev.rafex.ether.observability.core.probe.ProbeCheck;
import dev.rafex.ether.observability.core.probe.ProbeKind;
import dev.rafex.ether.observability.core.probe.ProbeResult;
import dev.rafex.ether.observability.core.probe.ProbeStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

public class HealthHandler extends NonBlockingResourceHandler {

	private static final JsonCodec JSON_CODEC = JsonUtils.codec();
	private static final JettyApiResponses RESPONSES = new JettyApiResponses(JSON_CODEC);

	private final List<ProbeCheck> probes;

	public HealthHandler() {
		this(null);
	}

	public HealthHandler(final DataSource dataSource) {
		super(JSON_CODEC);
		if (dataSource != null) {
			probes = List.of(dbProbe(dataSource));
		} else {
			probes = List.of();
		}
	}

	@Override
	protected String basePath() {
		return "/health";
	}

	@Override
	protected List<Route> routes() {
		return List.of(Route.of("/", Set.of("GET")));
	}

	@Override
	public boolean get(final dev.rafex.ether.http.core.HttpExchange x) {
		final var jx = asJetty(x);

		final var report = ProbeAggregator.aggregate(ProbeKind.HEALTH, probes);
		final var overallStatus = report.status();

		final var checksMap = new LinkedHashMap<String, Object>();
		for (final var result : report.results()) {
			checksMap.put(result.name(), Map.of("status", result.status().name(), "detail",
					result.detail() != null ? result.detail() : ""));
		}

		final var body = new LinkedHashMap<String, Object>();
		body.put("status", overallStatus.name());
		body.put("timestamp", Instant.now().toString());
		if (!checksMap.isEmpty()) {
			body.put("checks", checksMap);
		}

		final int httpStatus = overallStatus == ProbeStatus.DOWN ? 503 : 200;
		RESPONSES.json(jx.response(), jx.callback(), httpStatus, body);
		return true;
	}

	@Override
	public Set<String> supportedMethods() {
		return Set.of("GET");
	}

	private static ProbeCheck dbProbe(final DataSource ds) {
		return () -> {
			try (var conn = ds.getConnection()) {
				final var ok = conn.isValid(1);
				return new ProbeResult("database", ProbeKind.HEALTH, ok ? ProbeStatus.UP : ProbeStatus.DOWN,
						ok ? "connected" : "validation failed");
			} catch (final Exception e) {
				return new ProbeResult("database", ProbeKind.HEALTH, ProbeStatus.DOWN, e.getMessage());
			}
		};
	}

	private static JettyHttpExchange asJetty(final dev.rafex.ether.http.core.HttpExchange x) {
		return (JettyHttpExchange) x;
	}
}
