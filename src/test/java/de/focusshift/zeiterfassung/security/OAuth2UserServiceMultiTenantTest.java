package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.JWT_BEARER;
import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;
import static org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.SUB;

@ExtendWith(MockitoExtension.class)
class OAuth2UserServiceMultiTenantTest {

    private OAuth2UserServiceMultiTenant sut;

    @Mock
    private OidcUserService oidcUserService;
    @Mock
    private TenantUserService tenantUserService;

    @BeforeEach
    void setUp() {
        sut = new OAuth2UserServiceMultiTenant(oidcUserService, tenantUserService);
    }

    @Test
    void ensureOriginalOidcUserWhenUserDoesNotExistYet() {

        final Map<String, Object> claims = Map.of(SUB, "uuid");

        final ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("most-awesome-tenant").authorizationGrantType(JWT_BEARER).build();
        final OAuth2AccessToken accessToken = new OAuth2AccessToken(BEARER, "token-value", Instant.now(), Instant.now());
        final OidcIdToken oidcToken = OidcIdToken.withTokenValue("token-value").claims(map -> map.putAll(claims)).build();
        final OidcUserRequest oidcUserRequest = new OidcUserRequest(clientRegistration, accessToken, oidcToken);

        final DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("remote-role")), oidcToken, new OidcUserInfo(claims));
        when(oidcUserService.loadUser(oidcUserRequest)).thenReturn(oidcUser);

        when(tenantUserService.findById(new UserId("uuid"))).thenReturn(Optional.empty());

        final OidcUser actual = sut.loadUser(oidcUserRequest);
        assertThat(actual).isSameAs(oidcUser);
    }

    @Test
    void ensureMergedRemoteAndApplicationAuthorities() {

        final Map<String, Object> claims = Map.of(SUB, "uuid");

        final ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("most-awesome-tenant").authorizationGrantType(JWT_BEARER).build();
        final OAuth2AccessToken accessToken = new OAuth2AccessToken(BEARER, "token-value", Instant.now(), Instant.now());
        final OidcIdToken oidcToken = OidcIdToken.withTokenValue("token-value").claims(map -> map.putAll(claims)).build();
        final OidcUserRequest oidcUserRequest = new OidcUserRequest(clientRegistration, accessToken, oidcToken);

        final DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("remote-role")), oidcToken, new OidcUserInfo(claims));
        when(oidcUserService.loadUser(oidcUserRequest)).thenReturn(oidcUser);

        when(tenantUserService.findById(new UserId("uuid"))).thenReturn(Optional.of(anyTenantUser("uuid", Set.of(ZEITERFASSUNG_VIEW_REPORT_ALL))));

        final OidcUser actual = sut.loadUser(oidcUserRequest);

        assertThat(actual.getAuthorities().stream().map(GrantedAuthority::getAuthority))
            .containsExactlyInAnyOrder("remote-role", ZEITERFASSUNG_VIEW_REPORT_ALL.name());
    }

    @Test
    void ensureSecurityContextIsSet() {

        final Map<String, Object> claims = Map.of(SUB, "uuid");

        final ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("most-awesome-tenant").authorizationGrantType(JWT_BEARER).build();
        final OAuth2AccessToken accessToken = new OAuth2AccessToken(BEARER, "token-value", Instant.now(), Instant.now());
        final OidcIdToken oidcToken = OidcIdToken.withTokenValue("token-value").claims(map -> map.putAll(claims)).build();
        final OidcUserRequest oidcUserRequest = new OidcUserRequest(clientRegistration, accessToken, oidcToken);

        final DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("remote-role")), oidcToken, new OidcUserInfo(claims));
        when(oidcUserService.loadUser(oidcUserRequest)).thenReturn(oidcUser);

        when(tenantUserService.findById(new UserId("uuid"))).thenAnswer(invocation -> {

            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isInstanceOf(OAuth2AuthenticationToken.class);

            final String tenantId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            assertThat(tenantId).isEqualTo("most-awesome-tenant");

            return Optional.of(anyTenantUser("uuid"));
        });

        sut.loadUser(oidcUserRequest);

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    private TenantUser anyTenantUser(String id) {
        return anyTenantUser(id, Set.of());
    }

    private TenantUser anyTenantUser(String id, Set<SecurityRole> authorities) {
        return new TenantUser(id, 1L, "Bruce", "Wayne", new EMailAddress("batman@example.org"), authorities);
    }
}
