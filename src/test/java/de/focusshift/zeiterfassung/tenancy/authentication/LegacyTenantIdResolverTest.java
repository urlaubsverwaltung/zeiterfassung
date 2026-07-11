package de.focusshift.zeiterfassung.tenancy.authentication;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegacyTenantIdResolverTest {

    private LegacyTenantIdResolver sut;

    @BeforeEach
    void setUp() {
        sut = new LegacyTenantIdResolver(new LegacyTenantIdResolverProperties());
    }

    @Test
    void ensureResolveOAuth2AuthenticationTokenReturnsTenantIdFromAuthorizedClientRegistrationId() {

        final OAuth2AuthenticationToken token = mock(OAuth2AuthenticationToken.class);
        when(token.getAuthorizedClientRegistrationId()).thenReturn("b0838c26");

        final Optional<TenantId> actual = sut.resolve(token);

        assertThat(actual).contains(new TenantId("b0838c26"));
    }

    @Test
    void ensureResolveOAuth2LoginAuthenticationTokenReturnsTenantIdFromRegistrationId() {

        final ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("a154bc4e")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientId("x")
            .redirectUri("http://x")
            .authorizationUri("http://x")
            .tokenUri("http://x")
            .build();

        final OAuth2LoginAuthenticationToken token = mock(OAuth2LoginAuthenticationToken.class);
        when(token.getClientRegistration()).thenReturn(clientRegistration);

        final Optional<TenantId> actual = sut.resolve(token);

        assertThat(actual).contains(new TenantId("a154bc4e"));
    }

    @Test
    void ensureResolveOidcUserAuthorityParsesTenantIdFromIssuerPathSegment() {

        final OidcIdToken idToken = OidcIdToken.withTokenValue("tv")
            .subject("sub")
            .claim("iss", "http://localhost:8090/auth/realms/b0838c26")
            .build();

        final OidcUserAuthority authority = mock(OidcUserAuthority.class);
        when(authority.getIdToken()).thenReturn(idToken);

        final Optional<TenantId> actual = sut.resolve(authority);

        assertThat(actual).contains(new TenantId("b0838c26"));
    }

    @Test
    void ensureResolveOAuth2AuthenticationTokenReturnsEmptyForNonTenantRegistrationId() {

        final OAuth2AuthenticationToken token = mock(OAuth2AuthenticationToken.class);
        when(token.getAuthorizedClientRegistrationId()).thenReturn("master");

        final Optional<TenantId> actual = sut.resolve(token);

        assertThat(actual).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://localhost:8090/auth/realms/master", // shared / non-tenant realm - rejected by the pattern
        "not-a-url",                                // malformed issuer
        "http://localhost:8090/auth/foo/bar"        // issuer without the tenant marker segment
    })
    void ensureResolveOidcUserAuthorityReturnsEmptyForNonTenantIssuer(String issuer) {

        final OidcIdToken idToken = OidcIdToken.withTokenValue("tv")
            .subject("sub")
            .claim("iss", issuer)
            .build();

        final OidcUserAuthority authority = mock(OidcUserAuthority.class);
        when(authority.getIdToken()).thenReturn(idToken);

        final Optional<TenantId> actual = sut.resolve(authority);

        assertThat(actual).isEmpty();
    }
}
