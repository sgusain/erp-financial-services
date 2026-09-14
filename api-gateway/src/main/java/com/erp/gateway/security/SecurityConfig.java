package com.erp.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // Path-only public routes (all HTTP methods permitted).
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ReactiveAuthenticationManager jwtReactiveAuthenticationManager) {

        AuthenticationWebFilter authenticationWebFilter =
                new AuthenticationWebFilter(jwtReactiveAuthenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(new JwtServerAuthenticationConverter());
        authenticationWebFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        authenticationWebFilter.setRequiresAuthenticationMatcher(protectedPathsMatcher());
        authenticationWebFilter.setAuthenticationSuccessHandler((webFilterExchange, authentication) -> {
            ServerHttpRequest mutatedRequest = webFilterExchange.getExchange().getRequest().mutate()
                    .header("X-User-Email", authentication.getName())
                    .build();
            return webFilterExchange.getChain()
                    .filter(webFilterExchange.getExchange().mutate().request(mutatedRequest).build());
        });

        http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(auth -> auth
                .pathMatchers(PUBLIC_PATHS).permitAll()
                // Registration only - GET /api/users (list) and everything else on
                // /api/users still requires a valid JWT.
                .pathMatchers(HttpMethod.POST, "/api/users").permitAll()
                .anyExchange().authenticated()
            )
            .addFilterAt(authenticationWebFilter, org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION)
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable());
        return http.build();
    }

    private ServerWebExchangeMatcher protectedPathsMatcher() {
        List<ServerWebExchangeMatcher> publicMatchers = new java.util.ArrayList<>(
                java.util.Arrays.stream(PUBLIC_PATHS)
                        .map(PathPatternParserServerWebExchangeMatcher::new)
                        .map(ServerWebExchangeMatcher.class::cast)
                        .toList()
        );
        publicMatchers.add(new PathPatternParserServerWebExchangeMatcher("/api/users", HttpMethod.POST));

        ServerWebExchangeMatcher publicMatcher = new OrServerWebExchangeMatcher(publicMatchers);
        return new NegatedServerWebExchangeMatcher(publicMatcher);
    }
}
