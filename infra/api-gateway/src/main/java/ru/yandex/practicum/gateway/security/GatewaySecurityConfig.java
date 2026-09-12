package ru.yandex.practicum.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(
                        NoOpServerSecurityContextRepository.getInstance()
                )
                .httpBasic(Customizer.withDefaults())
                .authorizeExchange(exchanges -> exchanges

                        .pathMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        .pathMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        )
                        .permitAll()

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/inventory/**"
                        )
                        .permitAll()

                        .pathMatchers(HttpMethod.GET, "/api/orders")
                        .hasRole("ADMIN")

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/orders/by-email",
                                "/api/orders/*"
                        )
                        .hasRole("USER")

                        .pathMatchers(HttpMethod.POST, "/api/orders/**")
                        .hasRole("USER")

                        .pathMatchers(
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/inventory/**"
                        )
                        .hasRole("ADMIN")

                        .anyExchange()
                        .denyAll()
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(
            SecurityProperties securityProperties,
            PasswordEncoder passwordEncoder
    ) {
        List<UserDetails> users = securityProperties.users()
                .stream()
                .map(user -> User.withUsername(user.username())
                        .password(passwordEncoder.encode(user.password()))
                        .roles(user.roles().toArray(String[]::new))
                        .build())
                .toList();

        return new MapReactiveUserDetailsService(users);
    }
}