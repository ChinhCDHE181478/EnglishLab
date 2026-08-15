package fu.sep490.g23.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class CorsConfig {

    @Value("${englishlab.mail.base-url:}")
    private String webBaseUrl;

    @Value("${englishlab.cors.additional-origins:}")
    private String additionalOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> allowedOriginPatterns() {
        Set<String> patterns = new LinkedHashSet<>();
        patterns.add("http://localhost:*");
        patterns.add("http://127.0.0.1:*");
        addOrigin(patterns, webBaseUrl);
        if (additionalOrigins != null && !additionalOrigins.isBlank()) {
            for (String part : additionalOrigins.split(",")) {
                addOrigin(patterns, part);
            }
        }
        return new ArrayList<>(patterns);
    }

    private void addOrigin(Set<String> patterns, String rawOrigin) {
        if (rawOrigin == null) {
            return;
        }
        String origin = rawOrigin.trim().replaceAll("/+$", "");
        if (origin.isEmpty()) {
            return;
        }
        patterns.add(origin);
        if (origin.startsWith("https://www.")) {
            patterns.add("https://" + origin.substring("https://www.".length()));
        } else if (origin.startsWith("https://") && !origin.startsWith("https://www.")) {
            patterns.add("https://www." + origin.substring("https://".length()));
        }
    }
}
