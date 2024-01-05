package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

abstract class OAuth2TenantUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    OAuth2TenantUserService(OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.delegate = delegate;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        final OidcUser oidcUser = delegate.loadUser(userRequest);
        final Optional<TenantUser> tenantUser = loadTenantUser(userRequest, oidcUser);

        if (tenantUser.isEmpty()) {
            return oidcUser;
        }

        final List<GrantedAuthority> combinedAuthorities = Stream.concat(
            oidcUser.getAuthorities().stream(),
            tenantUser.get().authorities().stream().map(SecurityRole::authority)
        ).toList();

        return new DefaultOidcUser(combinedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    protected abstract Optional<TenantUser> loadTenantUser(OidcUserRequest oidcUserRequest, OidcUser oidcUser);
}
