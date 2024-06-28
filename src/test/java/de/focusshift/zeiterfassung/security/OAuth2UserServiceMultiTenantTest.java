package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.tenancy.user.UserStatus;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock
    private TenantContextHolder tenantContextHolder;

    private static OAuth2AccessToken oAuth2AccessToken() {
        final Instant now = Instant.now();
        return new OAuth2AccessToken(BEARER, "token-value", now, now.plusMillis(1));
    }

    @BeforeEach
    void setUp() {
        sut = new OAuth2UserServiceMultiTenant(oidcUserService, tenantUserService, tenantContextHolder);
    }

    @Test
    void ensureOriginalOidcUserWhenUserDoesNotExistYet() {

        final Map<String, Object> claims = Map.of(SUB, "uuid");
        final OidcIdToken oidcToken = oidcIdToken(claims);
        final OidcUserRequest oidcUserRequest = oidcUserRequest(oidcToken);
        final DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("remote-role")), oidcToken, new OidcUserInfo(claims));
        when(oidcUserService.loadUser(oidcUserRequest)).thenReturn(oidcUser);

        when(tenantUserService.findById(new UserId("uuid"))).thenReturn(Optional.empty());

        final OidcUser actual = sut.loadUser(oidcUserRequest);
        assertThat(actual).isSameAs(oidcUser);
    }

    @Test
    void ensureMergedRemoteAndApplicationAuthorities() {

        final Map<String, Object> claims = Map.of(SUB, "uuid");
        final OidcIdToken oidcToken = oidcIdToken(claims);
        final OidcUserRequest oidcUserRequest = oidcUserRequest(oidcToken);
        final DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("remote-role")), oidcToken, new OidcUserInfo(claims));
        when(oidcUserService.loadUser(oidcUserRequest)).thenReturn(oidcUser);

        when(tenantUserService.findById(new UserId("uuid"))).thenReturn(Optional.of(anyTenantUser("uuid", Set.of(ZEITERFASSUNG_VIEW_REPORT_ALL))));

        final OidcUser actual = sut.loadUser(oidcUserRequest);

        assertThat(actual.getAuthorities().stream().map(GrantedAuthority::getAuthority))
            .containsExactlyInAnyOrder("remote-role", ZEITERFASSUNG_VIEW_REPORT_ALL.name());
    }

    @Test
    void ensureTenantIdContextIsMaintained() {

        final Map<String, Object> claims = Map.of(SUB, "uuid");
        final OidcIdToken oidcToken = oidcIdToken(claims);
        final OidcUserRequest oidcUserRequest = oidcUserRequest(oidcToken);
        final DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("remote-role")), oidcToken, new OidcUserInfo(claims));
        when(oidcUserService.loadUser(oidcUserRequest)).thenReturn(oidcUser);

        when(tenantUserService.findById(new UserId("uuid"))).thenReturn(Optional.of(anyTenantUser("uuid")));

        OidcUser actual = sut.loadUser(oidcUserRequest);

        assertThat(actual).isEqualTo(oidcUser);
        InOrder inOrder = Mockito.inOrder(tenantContextHolder);
        inOrder.verify(tenantContextHolder).setTenantId(new TenantId("most-awesome-tenant"));
        inOrder.verify(tenantContextHolder).clear();
    }

    @Test
    void ensureNoUserIsReturnedWhenTenantIdIsUnavailable() {

        final Map<String, Object> claims = Map.of(SUB, "uuid");
        final OidcIdToken oidcToken = oidcIdToken(claims);
        final OidcUserRequest oidcUserRequest = oidcUserRequestWithoutTenantId(oidcToken);
        final DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("remote-role")), oidcToken, new OidcUserInfo(claims));

        sut.loadTenantUser(oidcUserRequest, oidcUser);

        verifyNoInteractions(tenantUserService);
        verify(tenantContextHolder, Mockito.never()).setTenantId(Mockito.any());
        verify(tenantContextHolder).clear();
    }

    private OidcUserRequest oidcUserRequest(OidcIdToken oidcToken) {
        final ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("most-awesome-tenant").authorizationGrantType(JWT_BEARER).build();
        return new OidcUserRequest(clientRegistration, oAuth2AccessToken(), oidcToken);
    }

    private OidcUserRequest oidcUserRequestWithoutTenantId(OidcIdToken oidcToken) {
        final ClientRegistration clientRegistration = Mockito.mock(ClientRegistration.class);
        when(clientRegistration.getRegistrationId()).thenReturn("");
        return new OidcUserRequest(clientRegistration, oAuth2AccessToken(), oidcToken);
    }

    private OidcIdToken oidcIdToken(Map<String, Object> claims) {
        return OidcIdToken.withTokenValue("token-value").claims(map -> map.putAll(claims)).build();
    }

    private TenantUser anyTenantUser(String id) {
        return anyTenantUser(id, Set.of());
    }

    private TenantUser anyTenantUser(String id, Set<SecurityRole> authorities) {
        Instant now = Instant.now();
        return new TenantUser(id, 1L, "Bruce", "Wayne", new EMailAddress("batman@example.org"), now, authorities, now, now, null, null, UserStatus.ACTIVE);
    }
}
