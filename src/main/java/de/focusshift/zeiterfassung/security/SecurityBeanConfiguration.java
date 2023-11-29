package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.security.oidc.OidcPersonAuthoritiesMapper;
import de.focusshift.zeiterfassung.security.oidc.OidcSecurityProperties;
import de.focusshift.zeiterfassung.security.oidc.claimmapper.RolesFromClaimMapper;
import de.focusshift.zeiterfassung.security.oidc.claimmapper.RolesFromClaimMappersInfusedOAuth2UserService;
import de.focusshift.zeiterfassung.security.oidc.claimmapper.RolesFromClaimMappersProperties;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.util.List;


@Configuration
@EnableConfigurationProperties({OidcSecurityProperties.class, RolesFromClaimMappersProperties.class})
class SecurityBeanConfiguration {

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(OidcSecurityProperties securityConfigurationProperties) {
        return new LoginUrlAuthenticationEntryPoint(securityConfigurationProperties.getLoginFormUrl());
    }

    @Bean
    OidcPersonAuthoritiesMapper oidcPersonAuthoritiesMapper(UserManagementService userManagementService) {
        return new OidcPersonAuthoritiesMapper(userManagementService);
    }

    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserOAuth2UserService(List<RolesFromClaimMapper> rolesFromClaimMappers) {
        final OidcUserService defaultOidcUserService = new OidcUserService();
        return new RolesFromClaimMappersInfusedOAuth2UserService(defaultOidcUserService, rolesFromClaimMappers);
    }

    @Bean
    OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler(final ClientRegistrationRepository clientRegistrationRepository, final OidcSecurityProperties securityConfigurationProperties) {
        final OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        oidcClientInitiatedLogoutSuccessHandler.setPostLogoutRedirectUri(securityConfigurationProperties.getPostLogoutRedirectUri());
        return oidcClientInitiatedLogoutSuccessHandler;
    }
}
