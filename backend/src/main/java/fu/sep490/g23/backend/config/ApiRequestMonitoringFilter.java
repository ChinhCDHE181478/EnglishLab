package fu.sap490.g23.backend.config;

import fu.sap490.g23.backend.service.admin.ApiMonitoringService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApiRequestMonitoringFilter extends OncePerRequestFilter {
    private final ApiMonitoringService monitoringService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();
        response.setHeader("X-Correlation-ID", correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            monitoringService.record(
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    (System.nanoTime() - startedAt) / 1_000_000,
                    correlationId
            );
        }
    }
}
