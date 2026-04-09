package com.yocaihua.wms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class GlobalCorsConfig {

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOriginPatterns;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        for (String pattern : parseAllowedOriginPatterns(allowedOriginPatterns)) {
            if (pattern != null && !pattern.isBlank()) {
                config.addAllowedOriginPattern(pattern.trim());
            }
        }
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean =
                new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return bean;
    }

    private List<String> parseAllowedOriginPatterns(String value) {
        List<String> patterns = new ArrayList<>();
        if (value == null || value.isBlank()) {
            patterns.add("http://localhost:*");
            patterns.add("http://127.0.0.1:*");
            return patterns;
        }

        String[] segments = value.split(",");
        for (String segment : segments) {
            if (segment != null) {
                String trimmed = segment.trim();
                if (!trimmed.isEmpty()) {
                    patterns.add(trimmed);
                }
            }
        }

        if (patterns.isEmpty()) {
            patterns.add("http://localhost:*");
            patterns.add("http://127.0.0.1:*");
        }
        return patterns;
    }
}
