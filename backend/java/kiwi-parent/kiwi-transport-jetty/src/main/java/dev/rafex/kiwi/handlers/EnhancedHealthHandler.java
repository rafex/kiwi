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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

public class EnhancedHealthHandler extends NonBlockingResourceHandler {

	private static final JsonCodec JSON_CODEC = JsonUtils.codec();
	private static final JettyApiResponses RESPONSES = new JettyApiResponses(JSON_CODEC);

	private final List<ProbeCheck> probes;

	public EnhancedHealthHandler(final DataSource dataSource) {
		super(JSON_CODEC);
		this.probes = createProbes(dataSource);
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
			checksMap.put(result.name(),
					Map.of("status", result.status().name(), 
						   "detail", result.detail() != null ? result.detail() : "",
						   "kind", result.kind().name()));
		}

		final var body = new LinkedHashMap<String, Object>();
		
		// Application metadata
		body.put("service", "kiwi");
		body.put("version", "0.1.0-SNAPSHOT");
		body.put("timestamp", Instant.now().toString());
		
		// Health status
		body.put("status", overallStatus.name());
		body.put("kind", ProbeKind.HEALTH.name());
		
		// System metrics
		body.put("system", getSystemMetrics());
		
		// Health checks
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

	private List<ProbeCheck> createProbes(final DataSource dataSource) {
		return List.of(
			dbProbe(dataSource),
			memoryProbe(),
			cpuProbe(),
			diskProbe(),
			applicationProbe()
		);
	}

	private static ProbeCheck dbProbe(final DataSource ds) {
		return () -> {
			try (var conn = ds.getConnection()) {
				final var ok = conn.isValid(1);
				return new ProbeResult("database", ProbeKind.READINESS, 
					ok ? ProbeStatus.UP : ProbeStatus.DOWN,
					ok ? "connected" : "validation failed");
			} catch (final Exception e) {
				return new ProbeResult("database", ProbeKind.READINESS, 
					ProbeStatus.DOWN, e.getMessage());
			}
		};
	}

	private static ProbeCheck memoryProbe() {
		return () -> {
			final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
			final var heapUsage = memoryBean.getHeapMemoryUsage();
			final var maxHeap = heapUsage.getMax();
			final var usedHeap = heapUsage.getUsed();
			
			final double usagePercentage = maxHeap > 0 ? 
				((double) usedHeap / maxHeap) * 100 : 0;
			
			final var status = usagePercentage > 90 ? ProbeStatus.DOWN : 
							  usagePercentage > 80 ? ProbeStatus.DEGRADED : ProbeStatus.UP;
			
			return new ProbeResult("memory", ProbeKind.HEALTH, status,
				String.format("Heap: %.1f%% used (%,d/%,d MB)", 
					usagePercentage, 
					usedHeap / (1024 * 1024),
					maxHeap / (1024 * 1024)));
		};
	}

	private static ProbeCheck cpuProbe() {
		return () -> {
			final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
				final double systemLoad = sunOsBean.getSystemCpuLoad();
				final double processLoad = sunOsBean.getProcessCpuLoad();
				
				final var status = systemLoad > 0.9 ? ProbeStatus.DEGRADED : ProbeStatus.UP;
				
				return new ProbeResult("cpu", ProbeKind.HEALTH, status,
					String.format("System: %.1f%%, Process: %.1f%%", 
						systemLoad * 100, processLoad * 100));
			}
			return new ProbeResult("cpu", ProbeKind.HEALTH, ProbeStatus.UP, 
				"CPU metrics not available");
		};
	}

	private static ProbeCheck diskProbe() {
		return () -> {
			try {
				final var file = new java.io.File(".");
				final var total = file.getTotalSpace();
				final var free = file.getFreeSpace();
				final var usable = file.getUsableSpace();
				
				final double freePercentage = total > 0 ? 
					((double) free / total) * 100 : 0;
				
				final var status = freePercentage < 10 ? ProbeStatus.DOWN : 
								  freePercentage < 20 ? ProbeStatus.DEGRADED : ProbeStatus.UP;
				
				return new ProbeResult("disk", ProbeKind.HEALTH, status,
					String.format("Free: %.1f%% (%,d/%,d MB)", 
						freePercentage,
						free / (1024 * 1024),
						total / (1024 * 1024)));
			} catch (final Exception e) {
				return new ProbeResult("disk", ProbeKind.HEALTH, ProbeStatus.DOWN, 
					e.getMessage());
			}
		};
	}

	private static ProbeCheck applicationProbe() {
		return () -> {
			final var runtime = Runtime.getRuntime();
			final var threadCount = Thread.activeCount();
			final var uptime = ManagementFactory.getRuntimeMXBean().getUptime();
			
			return new ProbeResult("application", ProbeKind.HEALTH, ProbeStatus.UP,
				String.format("Threads: %d, Uptime: %d minutes", 
					threadCount, uptime / (60 * 1000)));
		};
	}

	private Map<String, Object> getSystemMetrics() {
		final var metrics = new LinkedHashMap<String, Object>();
		
		try {
			final var runtime = Runtime.getRuntime();
			final var memoryBean = ManagementFactory.getMemoryMXBean();
			final var heapUsage = memoryBean.getHeapMemoryUsage();
			final var nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
			final var osBean = ManagementFactory.getOperatingSystemMXBean();
			final var runtimeBean = ManagementFactory.getRuntimeMXBean();
			
			metrics.put("jvm", Map.of(
				"name", runtimeBean.getVmName(),
				"version", runtimeBean.getVmVersion(),
				"vendor", runtimeBean.getVmVendor(),
				"uptime", runtimeBean.getUptime()
			));
			
			metrics.put("memory", Map.of(
				"heap", Map.of(
					"used", heapUsage.getUsed(),
					"committed", heapUsage.getCommitted(),
					"max", heapUsage.getMax()
				),
				"nonHeap", Map.of(
					"used", nonHeapUsage.getUsed(),
					"committed", nonHeapUsage.getCommitted(),
					"max", nonHeapUsage.getMax()
				),
				"availableProcessors", runtime.availableProcessors(),
				"totalMemory", runtime.totalMemory(),
				"freeMemory", runtime.freeMemory(),
				"maxMemory", runtime.maxMemory()
			));
			
			metrics.put("os", Map.of(
				"name", osBean.getName(),
				"version", osBean.getVersion(),
				"architecture", osBean.getArch(),
				"availableProcessors", osBean.getAvailableProcessors(),
				"systemLoadAverage", osBean.getSystemLoadAverage()
			));
			
		} catch (final Exception e) {
			metrics.put("error", "Failed to collect system metrics: " + e.getMessage());
		}
		
		return metrics;
	}

	private static JettyHttpExchange asJetty(final dev.rafex.ether.http.core.HttpExchange x) {
		return (JettyHttpExchange) x;
	}
}