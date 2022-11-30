package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.user.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityWebConfiguration {

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler;
    private final OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;
    private final CurrentUserProvider currentUserProvider;
    private final SessionService sessionService;

    @Autowired
    SecurityWebConfiguration(AuthenticationEntryPoint authenticationEntryPoint,
                             AuthenticationSuccessHandler authenticationSuccessHandler,
                             OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler,
                             OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
                             CurrentUserProvider currentUserProvider, SessionService sessionService) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.oidcClientInitiatedLogoutSuccessHandler = oidcClientInitiatedLogoutSuccessHandler;
        this.oidcUserService = oidcUserService;
        this.currentUserProvider = currentUserProvider;
        this.sessionService = sessionService;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        //@formatter:off
        http
            .authorizeRequests()
                .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                .requestMatchers(EndpointRequest.to(PrometheusScrapeEndpoint.class)).permitAll()
                .antMatchers("/fonts/**/*", "/style.css").permitAll()
                .anyRequest().authenticated()
            .and()
            .oauth2Login()
                .successHandler(authenticationSuccessHandler)
                .userInfoEndpoint()
                .oidcUserService(oidcUserService);

        // exclude /actuator from csrf protection
        http.requestMatcher(request -> !request.getRequestURI().startsWith("/actuator"))
            .csrf()
            // disable lazy csrf token creation - see https://github.com/spring-projects/spring-security/issues/3906
            .csrfTokenRepository(new HttpSessionCsrfTokenRepository());

        // maybe we can remove the authenticationEntryPoint customization, because
        // we are just using a default like configuration
        http.exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint);

        http.logout()
            .logoutSuccessHandler(oidcClientInitiatedLogoutSuccessHandler);

        http
            .addFilterAfter(new ReloadAuthenticationAuthoritiesFilter(currentUserProvider, sessionService), BasicAuthenticationFilter.class);

        //@formatter:on
        return http.build();
    }
}
