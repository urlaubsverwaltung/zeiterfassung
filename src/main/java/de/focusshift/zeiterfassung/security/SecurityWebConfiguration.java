package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;

@Configuration
@EnableMethodSecurity
public class SecurityWebConfiguration {

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final SessionService sessionService;
    private final UserManagementService userManagementService;

    @Autowired
    SecurityWebConfiguration(AuthenticationEntryPoint authenticationEntryPoint,
                             OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler,
                             ClientRegistrationRepository clientRegistrationRepository,
                             SessionService sessionService,
                             UserManagementService userManagementService) {

        this.authenticationEntryPoint = authenticationEntryPoint;
        this.oidcClientInitiatedLogoutSuccessHandler = oidcClientInitiatedLogoutSuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.sessionService = sessionService;
        this.userManagementService = userManagementService;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, DelegatingSecurityContextRepository securityContextRepository, TenantContextHolder tenantContextHolder) throws Exception {
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
                .requestMatchers("/", "/**").hasAuthority(ZEITERFASSUNG_USER.name())
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

        http.securityContext(securityContext -> securityContext.securityContextRepository(securityContextRepository));
        http.addFilterAfter(new ReloadAuthenticationAuthoritiesFilter(userManagementService, sessionService, securityContextRepository, tenantContextHolder), BasicAuthenticationFilter.class);

        //@formatter:on
        return http.build();
    }

    @Bean
    DelegatingSecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
            new RequestAttributeSecurityContextRepository(),
            new HttpSessionSecurityContextRepository()
        );
    }
}
