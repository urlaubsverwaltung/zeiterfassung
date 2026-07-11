package de.focusshift.zeiterfassung.tenancy.authentication;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ClaimTenantIdResolverTest {

    private ClaimTenantIdResolver sut;

    @BeforeEach
    void setUp() {
        sut = new ClaimTenantIdResolver(new ClaimTenantIdResolverProperties());
    }

    private static OidcIdToken idTokenWithClaim(Object claimValue) {
        return OidcIdToken.withTokenValue("tv")
            .issuer("https://issuer.example")
            .subject("sub")
            .claim("tenant_id", claimValue)
            .build();
    }

    private static OidcIdToken idTokenWithoutClaim() {
        return OidcIdToken.withTokenValue("tv")
            .issuer("https://issuer.example")
            .subject("sub")
            .claim("some_other_claim", "value")
            .build();
    }

    @Nested
    class ResolveOidcUserAuthority {

        @Test
        void ensureStringClaimInIdTokenIsResolved() {
            final OidcUserAuthority authority = new OidcUserAuthority(idTokenWithClaim("acme"), null);

            final Optional<TenantId> actual = sut.resolve(authority);

            assertThat(actual).contains(new TenantId("acme"));
        }

        @Test
        void ensureSingleElementCollectionClaimIsResolved() {
            final OidcUserAuthority authority = new OidcUserAuthority(idTokenWithClaim(List.of("t1")), null);

            final Optional<TenantId> actual = sut.resolve(authority);

            assertThat(actual).contains(new TenantId("t1"));
        }

        @Test
        void ensureMultiElementCollectionClaimResolvesToEmptyAndDoesNotPickOne() {
            final OidcUserAuthority authority = new OidcUserAuthority(idTokenWithClaim(List.of("a", "b")), null);

            final Optional<TenantId> actual = sut.resolve(authority);

            assertThat(actual)
                .isEmpty()
                .isNotEqualTo(Optional.of(new TenantId("a")))
                .isNotEqualTo(Optional.of(new TenantId("b")));
        }

        @Test
        void ensureMissingClaimResolvesToEmpty() {
            final OidcUserAuthority authority = new OidcUserAuthority(idTokenWithoutClaim(), null);

            final Optional<TenantId> actual = sut.resolve(authority);

            assertThat(actual).isEmpty();
        }

        @Test
        void ensureIdTokenClaimWinsOverUserInfoClaim() {
            final OidcIdToken idToken = idTokenWithClaim("id-token-tenant");
            final OidcUserInfo userInfo = new OidcUserInfo(Map.of("tenant_id", "user-info-tenant"));
            final OidcUserAuthority authority = new OidcUserAuthority(idToken, userInfo);

            final Optional<TenantId> actual = sut.resolve(authority);

            assertThat(actual).contains(new TenantId("id-token-tenant"));
        }

        @Test
        void ensureFallsBackToUserInfoClaimWhenIdTokenClaimMissing() {
            final OidcIdToken idToken = idTokenWithoutClaim();
            final OidcUserInfo userInfo = new OidcUserInfo(Map.of("tenant_id", "user-info-tenant"));
            final OidcUserAuthority authority = new OidcUserAuthority(idToken, userInfo);

            final Optional<TenantId> actual = sut.resolve(authority);

            assertThat(actual).contains(new TenantId("user-info-tenant"));
        }

        @Test
        void ensureCustomClaimNameIsHonoured() {
            final ClaimTenantIdResolverProperties properties = new ClaimTenantIdResolverProperties();
            properties.setClaimName("org");
            final ClaimTenantIdResolver customSut = new ClaimTenantIdResolver(properties);

            final OidcIdToken idToken = OidcIdToken.withTokenValue("tv")
                .issuer("https://issuer.example")
                .subject("sub")
                .claim("org", "custom-tenant")
                .build();
            final OidcUserAuthority authority = new OidcUserAuthority(idToken, null);

            final Optional<TenantId> actual = customSut.resolve(authority);

            assertThat(actual).contains(new TenantId("custom-tenant"));
        }
    }

    @Nested
    class ResolveOAuth2AuthenticationToken {

        @Test
        void ensureStringClaimInIdTokenIsResolved() {
            final OidcUser oidcUser = new OidcUserAuthorityBackedOidcUser(idTokenWithClaim("acme"), null);
            final OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "keycloak");

            final Optional<TenantId> actual = sut.resolve(token);

            assertThat(actual).contains(new TenantId("acme"));
        }

        @Test
        void ensureMissingClaimResolvesToEmpty() {
            final OidcUser oidcUser = new OidcUserAuthorityBackedOidcUser(idTokenWithoutClaim(), null);
            final OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "keycloak");

            final Optional<TenantId> actual = sut.resolve(token);

            assertThat(actual).isEmpty();
        }

        @Test
        void ensureIdTokenClaimWinsOverUserInfoClaim() {
            final OidcUserInfo userInfo = new OidcUserInfo(Map.of("tenant_id", "user-info-tenant"));
            final OidcUser oidcUser = new OidcUserAuthorityBackedOidcUser(idTokenWithClaim("id-token-tenant"), userInfo);
            final OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "keycloak");

            final Optional<TenantId> actual = sut.resolve(token);

            assertThat(actual).contains(new TenantId("id-token-tenant"));
        }
    }

    private static final class OidcUserAuthorityBackedOidcUser extends DefaultOidcUser {

        @Serial
        private static final long serialVersionUID = 1L;

        OidcUserAuthorityBackedOidcUser(OidcIdToken idToken, OidcUserInfo userInfo) {
            super(List.of(new OidcUserAuthority(idToken, userInfo)), idToken, userInfo);
        }
    }
}
