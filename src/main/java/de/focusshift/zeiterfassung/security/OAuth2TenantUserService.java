package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

abstract class OAuth2TenantUserService implements OAuth2UserService<OidcUserRequest, CurrentOidcUser> {

    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    OAuth2TenantUserService(OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.delegate = delegate;
    }

    @Override
    public CurrentOidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        final OidcUser oidcUser = delegate.loadUser(userRequest);
        final Optional<TenantUser> maybeTenantUser = loadTenantUser(userRequest, oidcUser);

        if (maybeTenantUser.isEmpty()) {
            return new CurrentOidcUser(oidcUser, List.of(), oidcUser.getAuthorities());
        }

        final TenantUser tenantUser = maybeTenantUser.get();

        if (!tenantUser.isActive()) {
            // prevent login of non-active users
            throw new UserNotActiveException(tenantUser.id());
        }

        final UserLocalId userLocalId = new UserLocalId(tenantUser.localId());
        final Collection<GrantedAuthority> applicationAuthorities = tenantUser.authorities().stream().map(SecurityRole::authority).toList();

        return new CurrentOidcUser(oidcUser, applicationAuthorities, oidcUser.getAuthorities(), userLocalId);
    }

    protected abstract Optional<TenantUser> loadTenantUser(OidcUserRequest oidcUserRequest, OidcUser oidcUser);

    static class UserNotActiveException extends AccountStatusException {
        @Serial
        private static final long serialVersionUID = 8861681937476923606L;

        public UserNotActiveException(String userId) {
            super("user with id=%s is not active!".formatted(userId));
        }
    }
}
