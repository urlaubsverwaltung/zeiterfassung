package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;
import java.util.stream.Stream;

public class RolesFromClaimMappersInfusedOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private final List<RolesFromClaimMapper> claimMappers;

    public RolesFromClaimMappersInfusedOAuth2UserService(OAuth2UserService<OidcUserRequest, OidcUser> delegate, List<RolesFromClaimMapper> claimMappers) {
        this.delegate = delegate;
        this.claimMappers = claimMappers;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        final OidcUser oidcUser = delegate.loadUser(userRequest);

        final List<GrantedAuthority> combinedAuthorities = Stream.concat(
            oidcUser.getAuthorities().stream(),
            getAuthoritiesFromClaimMappers(oidcUser)
        ).toList();

        return new DefaultOidcUser(combinedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private Stream<GrantedAuthority> getAuthoritiesFromClaimMappers(OidcUser oidcUser) {
        return claimMappers.stream()
            .flatMap(rolesFromClaimMapper -> rolesFromClaimMapper.mapClaimToRoles(oidcUser.getClaims()).stream());
    }
}
