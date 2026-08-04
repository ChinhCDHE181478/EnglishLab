package fu.sap490.g23.backend.service.admin.impl;

import fu.sap490.g23.backend.dto.response.admin.ApiFailureResponse;
import fu.sap490.g23.backend.dto.response.admin.ApiMonitoringResponse;
import fu.sap490.g23.backend.dto.response.admin.ApiRouteMetricResponse;
import fu.sap490.g23.backend.service.admin.ApiMonitoringService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

@Service
public class ApiMonitoringServiceImpl implements ApiMonitoringService {
    private static final int MAX_FAILURES = 100;
    private static final int MAX_ROUTES_IN_RESPONSE = 20;

    private final JdbcTemplate jdbcTemplate;
    private final ConcurrentHashMap<String, RouteMetric> routeMetrics = new ConcurrentHashMap<>();
    private final ArrayDeque<ApiFailureResponse> recentFailures = new ArrayDeque<>();
    private final Object failureLock = new Object();

    public ApiMonitoringServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(String method, String requestPath, int status, long durationMs, String correlationId) {
        String route = normalizePath(requestPath);
        String normalizedMethod = method == null ? "UNKNOWN" : method.toUpperCase();
        RouteMetric metric = routeMetrics.computeIfAbsent(normalizedMethod + " " + route,
                ignored -> new RouteMetric(normalizedMethod, route));
        metric.requests.increment();
        metric.totalLatencyMs.add(Math.max(0, durationMs));
        metric.maximumLatencyMs.accumulate(Math.max(0, durationMs));
        if (status >= 400) {
            metric.errors.increment();
            synchronized (failureLock) {
                recentFailures.addFirst(ApiFailureResponse.builder()
                        .occurredAt(LocalDateTime.now())
                        .method(normalizedMethod)
                        .route(route)
                        .status(status)
                        .durationMs(durationMs)
                        .correlationId(sanitizeCorrelationId(correlationId))
                        .build());
                while (recentFailures.size() > MAX_FAILURES) recentFailures.removeLast();
            }
        }
    }

    @Override
    public ApiMonitoringResponse getSummary() {
        long totalRequests = routeMetrics.values().stream().mapToLong(item -> item.requests.sum()).sum();
        long totalErrors = routeMetrics.values().stream().mapToLong(item -> item.errors.sum()).sum();
        long totalLatency = routeMetrics.values().stream().mapToLong(item -> item.totalLatencyMs.sum()).sum();
        long maximumLatency = routeMetrics.values().stream().mapToLong(item -> item.maximumLatencyMs.get()).max().orElse(0);

        long databaseStartedAt = System.nanoTime();
        String databaseStatus;
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            databaseStatus = Integer.valueOf(1).equals(result) ? "UP" : "DEGRADED";
        } catch (Exception exception) {
            databaseStatus = "DOWN";
        }
        long databaseLatencyMs = (System.nanoTime() - databaseStartedAt) / 1_000_000;
        Runtime runtime = Runtime.getRuntime();

        List<ApiRouteMetricResponse> routes = routeMetrics.values().stream()
                .sorted(Comparator.comparingLong((RouteMetric item) -> item.requests.sum()).reversed())
                .limit(MAX_ROUTES_IN_RESPONSE)
                .map(this::toResponse)
                .toList();
        List<ApiFailureResponse> failures;
        synchronized (failureLock) {
            failures = recentFailures.stream().limit(30).toList();
        }

        return ApiMonitoringResponse.builder()
                .measuredAt(LocalDateTime.now())
                .applicationStatus("DOWN".equals(databaseStatus) ? "DEGRADED" : "UP")
                .databaseStatus(databaseStatus)
                .databaseLatencyMs(databaseLatencyMs)
                .uptimeSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000)
                .totalRequests(totalRequests)
                .totalErrors(totalErrors)
                .errorRatePercent(percent(totalErrors, totalRequests))
                .averageLatencyMs(totalRequests == 0 ? 0 : round((double) totalLatency / totalRequests))
                .maximumLatencyMs(maximumLatency)
                .usedHeapBytes(runtime.totalMemory() - runtime.freeMemory())
                .maximumHeapBytes(runtime.maxMemory())
                .availableProcessors(runtime.availableProcessors())
                .busiestRoutes(routes)
                .recentFailures(failures)
                .measurementScope("Số liệu request được thu thập từ lần khởi động backend gần nhất.")
                .build();
    }

    private ApiRouteMetricResponse toResponse(RouteMetric item) {
        long requests = item.requests.sum();
        long errors = item.errors.sum();
        return ApiRouteMetricResponse.builder()
                .method(item.method)
                .route(item.route)
                .requestCount(requests)
                .errorCount(errors)
                .errorRatePercent(percent(errors, requests))
                .averageLatencyMs(requests == 0 ? 0 : round((double) item.totalLatencyMs.sum() / requests))
                .maximumLatencyMs(item.maximumLatencyMs.get())
                .build();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        String withoutQuery = path.split("\\?", 2)[0];
        return withoutQuery
                .replaceAll("/\\d+(?=/|$)", "/{id}")
                .replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F-]{27,}(?=/|$)", "/{uuid}")
                .replaceAll("//+", "/");
    }

    private String sanitizeCorrelationId(String value) {
        if (value == null || value.isBlank()) return null;
        String sanitized = value.replaceAll("[^a-zA-Z0-9._-]", "");
        return sanitized.substring(0, Math.min(80, sanitized.length()));
    }

    private double percent(long numerator, long denominator) {
        return denominator == 0 ? 0 : round(numerator * 100.0 / denominator);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static final class RouteMetric {
        private final String method;
        private final String route;
        private final LongAdder requests = new LongAdder();
        private final LongAdder errors = new LongAdder();
        private final LongAdder totalLatencyMs = new LongAdder();
        private final LongAccumulator maximumLatencyMs = new LongAccumulator(Long::max, 0);

        private RouteMetric(String method, String route) {
            this.method = method;
            this.route = route;
        }
    }
}
