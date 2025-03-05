package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

public class RolesFromClaimMappersInfusedOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuth2UserService<OidcUserRequest, CurrentOidcUser> delegate;
    private final List<RolesFromClaimMapper> claimMappers;

    public RolesFromClaimMappersInfusedOAuth2UserService(OAuth2UserService<OidcUserRequest, CurrentOidcUser> delegate, List<RolesFromClaimMapper> claimMappers) {
        this.delegate = delegate;
        this.claimMappers = claimMappers;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        final CurrentOidcUser oidcUser = delegate.loadUser(userRequest);

        final Collection<GrantedAuthority> authoritiesFromClaims = getAuthoritiesFromClaimMappers(oidcUser).toList();
        final List<GrantedAuthority> oidcAuthorities = concat(oidcUser.getOidcAuthorities().stream(), authoritiesFromClaims.stream()).toList();

        return new CurrentOidcUser(oidcUser, oidcUser.getApplicationAuthorities(), oidcAuthorities, oidcUser.getUserLocalId().orElse(null));
    }

    private Stream<GrantedAuthority> getAuthoritiesFromClaimMappers(OidcUser oidcUser) {
        return claimMappers.stream()
            .flatMap(rolesFromClaimMapper -> rolesFromClaimMapper.mapClaimToRoles(oidcUser.getClaims()).stream());
    }
}
