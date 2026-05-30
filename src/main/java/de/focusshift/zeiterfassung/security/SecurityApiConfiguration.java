package de.focusshift.zeiterfassung.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static org.springframework.security.config.http.SessionCreationPolicy.NEVER;

@Configuration
class SecurityApiConfiguration {

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(final HttpSecurity http) {
        return http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(requests ->
                requests
                    .requestMatchers("/api/**").hasAuthority(ZEITERFASSUNG_USER.name())
                    .anyRequest().authenticated()
            ).sessionManagement(
                sessionManagement -> sessionManagement.sessionCreationPolicy(NEVER)
            ).build();
    }
}
