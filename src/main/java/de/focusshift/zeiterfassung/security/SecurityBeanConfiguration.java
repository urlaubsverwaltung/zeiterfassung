package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.registration.oidc.SecurityConfigurationProperties;
import de.focusshift.zeiterfassung.security.oidc.claimmapper.ClaimBasedOAuth2UserService;
import de.focusshift.zeiterfassung.security.oidc.claimmapper.RolesFromClaimMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;


@Configuration
@EnableConfigurationProperties(SecurityConfigurationProperties.class)
class SecurityBeanConfiguration {

    @Bean
    GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        // All roles have ROLE_-prefix and roles from keycloak are mapped
        // to upper case style to be spring security-conform
        final SimpleAuthorityMapper authorityMapper = new SimpleAuthorityMapper();
        authorityMapper.setConvertToUpperCase(true);
        return authorityMapper;
    }

    @Bean
    OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler(final ClientRegistrationRepository clientRegistrationRepository, final SecurityConfigurationProperties securityConfigurationProperties) {
        final OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        oidcClientInitiatedLogoutSuccessHandler.setPostLogoutRedirectUri(securityConfigurationProperties.getPostLogoutRedirectUri());
        return oidcClientInitiatedLogoutSuccessHandler;
    }

    // oidc multi tenant realm login related
    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(RolesFromClaimMapper rolesFromClaimMapper) {
        final OidcUserService defaultOidcUserService = new OidcUserService();
        return new ClaimBasedOAuth2UserService(defaultOidcUserService, rolesFromClaimMapper);
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(SecurityConfigurationProperties securityConfigurationProperties) {
        return new LoginUrlAuthenticationEntryPoint(securityConfigurationProperties.getLoginFormUrl());
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SavedRequestAwareAuthenticationSuccessHandler();
    }

}
