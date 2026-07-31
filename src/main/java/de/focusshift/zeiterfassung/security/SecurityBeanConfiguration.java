package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.security.oidc.OidcSecurityProperties;
import de.focusshift.zeiterfassung.security.oidc.claimmapper.RolesFromClaimMapper;
import de.focusshift.zeiterfassung.security.oidc.claimmapper.RolesFromClaimMappersInfusedOAuth2UserService;
import de.focusshift.zeiterfassung.security.oidc.claimmapper.RolesFromClaimMappersProperties;
import de.focusshift.zeiterfassung.tenancy.authentication.TenantIdProvider;
import de.focusshift.zeiterfassung.tenancy.configuration.multi.ConditionalOnMultiTenantMode;
import de.focusshift.zeiterfassung.tenancy.configuration.single.ConditionalOnSingleTenantMode;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
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
import java.util.function.Predicate;

@Configuration
@EnableConfigurationProperties({OidcSecurityProperties.class, RolesFromClaimMappersProperties.class})
class SecurityBeanConfiguration {

    private final OidcSecurityProperties oidcSecurityProperties;

    SecurityBeanConfiguration(OidcSecurityProperties oidcSecurityProperties) {
        this.oidcSecurityProperties = oidcSecurityProperties;
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return new LoginUrlAuthenticationEntryPoint(oidcSecurityProperties.getLoginFormUrl());
    }

    @Bean
    @ConditionalOnSingleTenantMode
    OAuth2UserService<OidcUserRequest, OidcUser> oAuth2UserServiceSingleTenant(TenantUserService tenantUserService, List<RolesFromClaimMapper> rolesFromClaimMappers) {
        final OidcUserService defaultOidcUserService = new OidcUserService();

        if (oidcSecurityProperties.retrieveUserInfo()) {
            forceRetrieveUserInfo(defaultOidcUserService);
        }

        final OAuth2UserServiceSingleTenant userService = new OAuth2UserServiceSingleTenant(defaultOidcUserService, tenantUserService);
        return new RolesFromClaimMappersInfusedOAuth2UserService(userService, rolesFromClaimMappers);
    }

    @Bean
    @ConditionalOnMultiTenantMode
    OAuth2UserService<OidcUserRequest, OidcUser> oAuth2UserServiceMultiTenant(TenantUserService tenantUserService, TenantContextHolder tenantContextHolder, TenantIdProvider tenantIdProvider, List<RolesFromClaimMapper> rolesFromClaimMappers) {
        final OidcUserService defaultOidcUserService = new OidcUserService();

        if (oidcSecurityProperties.retrieveUserInfo()) {
            forceRetrieveUserInfo(defaultOidcUserService);
        }

        final OAuth2UserServiceMultiTenant userService = new OAuth2UserServiceMultiTenant(defaultOidcUserService, tenantUserService, tenantContextHolder, tenantIdProvider);
        return new RolesFromClaimMappersInfusedOAuth2UserService(userService, rolesFromClaimMappers);
    }

    @Bean
    OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler(final ClientRegistrationRepository clientRegistrationRepository, final OidcSecurityProperties securityConfigurationProperties) {
        final OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        oidcClientInitiatedLogoutSuccessHandler.setPostLogoutRedirectUri(securityConfigurationProperties.getPostLogoutRedirectUri());
        return oidcClientInitiatedLogoutSuccessHandler;
    }

    private static void forceRetrieveUserInfo(OidcUserService defaultOidcUserService) {
        Predicate<OidcUserRequest> retrieveUserInfo = (oidcUserRequest) -> true;
        defaultOidcUserService.setRetrieveUserInfo(retrieveUserInfo);
    }
}
