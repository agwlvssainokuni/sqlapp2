/*
 * Copyright 2025 agwlvssainokuni
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

package cherry.sqlapp2.config;

import cherry.sqlapp2.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final boolean csrfEnabled;
    private final boolean requireHttps;
    private final String frameOptions;
    private final String contentTypeOptions;
    private final String xssProtection;
    private final String referrerPolicy;
    private final String strictTransportSecurity;
    private final String contentSecurityPolicy;
    private final String[] allowedOrigins;
    private final String[] allowedMethods;
    private final String[] allowedHeaders;
    private final boolean allowCredentials;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${app.security.csrf.enabled:false}") boolean csrfEnabled,
            @Value("${app.security.require-https:false}") boolean requireHttps,
            @Value("${app.security.frame-options:SAMEORIGIN}") String frameOptions,
            @Value("${app.security.content-type-options:nosniff}") String contentTypeOptions,
            @Value("${app.security.xss-protection:}") String xssProtection,
            @Value("${app.security.referrer-policy:}") String referrerPolicy,
            @Value("${app.security.strict-transport-security:}") String strictTransportSecurity,
            @Value("${app.security.content-security-policy:}") String contentSecurityPolicy,
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080}") String[] allowedOrigins,
            @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}") String[] allowedMethods,
            @Value("${app.cors.allowed-headers:*}") String[] allowedHeaders,
            @Value("${app.cors.allow-credentials:true}") boolean allowCredentials
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.csrfEnabled = csrfEnabled;
        this.requireHttps = requireHttps;
        this.frameOptions = frameOptions;
        this.contentTypeOptions = contentTypeOptions;
        this.xssProtection = xssProtection;
        this.referrerPolicy = referrerPolicy;
        this.strictTransportSecurity = strictTransportSecurity;
        this.contentSecurityPolicy = contentSecurityPolicy;
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
        this.allowCredentials = allowCredentials;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSRF Configuration
        if (csrfEnabled) {
            http.csrf(csrf -> csrf
                    .ignoringRequestMatchers("/h2-console/**")
            );
        } else {
            http.csrf(AbstractHttpConfigurer::disable);
        }

        // CORS Configuration
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // HTTPS Redirect
        if (requireHttps) {
            http.redirectToHttps(https -> {
            });
        }

        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authz -> authz
                        // 認証系APIは認証不要
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout"
                        ).permitAll()
                        // ヘルスチェックやH2コンソールは認証不要
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // Actuatorエンドポイントは認証不要
                        .requestMatchers("/actuator/**").permitAll()
                        // OpenAPI/Swagger UIは認証不要（開発・テスト用）
                        .requestMatchers(
                                "/api/v3/api-docs/**",
                                "/api/swagger-ui/**",
                                "/api/swagger-ui.html"
                        ).permitAll()
                        // 静的コンテンツ(SPA)は認証不要
                        .requestMatchers(
                                "/", "/index.html", "/assets/**",
                                "/*.png", "/*.ico", "/*.svg",
                                "/login", "/register",
                                "/dashboard",
                                "/sql", "/connections", "/schema",
                                "/queries", "/history", "/builder"
                        ).permitAll()
                        // その他のリクエストは認証が必要
                        .anyRequest().authenticated()
                )
                .headers(headers -> {
                    // Frame Options
                    if ("DENY".equalsIgnoreCase(frameOptions)) {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                    } else {
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
                    }

                    // Content Type Options
                    if ("nosniff".equalsIgnoreCase(contentTypeOptions)) {
                        headers.contentTypeOptions(contentTypeConfig -> {
                        });
                    }

                    // XSS Protection
                    if (!xssProtection.isEmpty()) {
                        headers.addHeaderWriter((request, response) -> {
                            response.setHeader("X-XSS-Protection", xssProtection);
                        });
                    }

                    // Referrer Policy
                    if (!referrerPolicy.isEmpty()) {
                        headers.referrerPolicy(referrerPolicyConfig -> {
                            referrerPolicyConfig.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.valueOf(
                                    referrerPolicy.toUpperCase().replace("-", "_")));
                        });
                    }

                    // Strict Transport Security
                    if (!strictTransportSecurity.isEmpty()) {
                        headers.addHeaderWriter((request, response) -> {
                            if (request.isSecure()) {
                                response.setHeader("Strict-Transport-Security", strictTransportSecurity);
                            }
                        });
                    }

                    // Content Security Policy
                    if (!contentSecurityPolicy.isEmpty()) {
                        headers.addHeaderWriter((request, response) -> {
                            response.setHeader("Content-Security-Policy", contentSecurityPolicy);
                        });
                    }
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
