package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;

class OAuth2UserServiceSingleTenant extends OAuth2TenantUserService {

    private final TenantUserService tenantUserService;

    OAuth2UserServiceSingleTenant(OAuth2UserService<OidcUserRequest, OidcUser> delegate, TenantUserService tenantUserService) {
        super(delegate);
        this.tenantUserService = tenantUserService;
    }

    @Override
    protected Optional<TenantUser> loadTenantUser(OidcUserRequest oidcUserRequest, OidcUser oidcUser) {
        return tenantUserService.findById(new UserId(oidcUser.getUserInfo().getSubject()));
    }
}
