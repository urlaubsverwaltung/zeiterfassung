package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;

class OAuth2UserServiceMultiTenant extends OAuth2TenantUserService {

    private final TenantUserService tenantUserService;

    OAuth2UserServiceMultiTenant(OAuth2UserService<OidcUserRequest, OidcUser> delegate, TenantUserService tenantUserService) {
        super(delegate);
        this.tenantUserService = tenantUserService;
    }

    @Override
    protected Optional<TenantUser> loadTenantUser(OidcUserRequest oidcUserRequest, OidcUser oidcUser) {
        try {
            // reading stuff from database requires an authentication in the securityContext
            // in order to get the clientRegistration for database row security queries.
            prepareSecurityContext(oidcUserRequest, oidcUser);
            return tenantUserService.findById(new UserId(oidcUser.getUserInfo().getSubject()));
        } finally {
            // reset securityContext to keep spring security mechanism
            clearSecurityContext();
        }
    }

    private void prepareSecurityContext(OidcUserRequest userRequest, OidcUser oidcUser) {

        final OAuth2AuthenticationToken authentication =
            new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), userRequest.getClientRegistration().getRegistrationId());

        final SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.setContext(new SecurityContextImpl());
    }
}
