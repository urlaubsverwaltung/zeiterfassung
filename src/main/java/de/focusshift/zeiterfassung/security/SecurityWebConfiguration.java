package de.focusshift.zeiterfassung.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
class SecurityWebConfiguration {

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    SecurityWebConfiguration(AuthenticationEntryPoint authenticationEntryPoint,
                             OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler,
                             ClientRegistrationRepository clientRegistrationRepository) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.oidcClientInitiatedLogoutSuccessHandler = oidcClientInitiatedLogoutSuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        //@formatter:off
        http
            .authorizeHttpRequests(authorizeHttpRequests ->
                authorizeHttpRequests
                .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                .requestMatchers(EndpointRequest.to(PrometheusScrapeEndpoint.class)).permitAll()
                .requestMatchers("/fonts/*/*", "/style.css").permitAll()
                .requestMatchers("/favicons/**").permitAll()
                .requestMatchers("/browserconfig.xml").permitAll()
                .requestMatchers("/site.webmanifest").permitAll()
                .anyRequest().authenticated()
            );

        http.oauth2Login(
            loginCustomizer -> loginCustomizer.authorizationEndpoint(
                endpointCustomizer -> endpointCustomizer.authorizationRequestResolver(new LoginHintAwareResolver(clientRegistrationRepository))
            )
        );

        // exclude /actuator from csrf protection
        http.securityMatcher(request -> !request.getRequestURI().startsWith("/actuator"))
            .csrf(csrfConfigurer -> csrfConfigurer.csrfTokenRepository(new HttpSessionCsrfTokenRepository()));

        // maybe we can remove the authenticationEntryPoint customization, because
        // we are just using a default like configuration
        http.exceptionHandling(
            handlingConfigurer -> handlingConfigurer.authenticationEntryPoint(authenticationEntryPoint)
        );

        http.logout(
            logoutCustomizer -> logoutCustomizer.logoutSuccessHandler(oidcClientInitiatedLogoutSuccessHandler)
        );

        //@formatter:on
        return http.build();
    }

}
