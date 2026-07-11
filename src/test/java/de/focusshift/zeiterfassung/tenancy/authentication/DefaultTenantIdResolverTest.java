package de.focusshift.zeiterfassung.tenancy.authentication;

import de.focusshift.zeiterfassung.tenancy.configuration.single.SingleTenantConfigurationProperties;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DefaultTenantIdResolverTest {

    @Test
    void ensureResolveReturnsDefaultTenantIdForOAuth2AuthenticationToken() {

        final DefaultTenantIdResolver sut = new DefaultTenantIdResolver(propertiesWithDefaultTenantId("default"));

        final OAuth2AuthenticationToken token = mock(OAuth2AuthenticationToken.class);
        final Optional<TenantId> actual = sut.resolve(token);

        assertThat(actual).contains(new TenantId("default"));
        verifyNoInteractions(token);
    }

    @Test
    void ensureResolveReturnsDefaultTenantIdForOAuth2LoginAuthenticationToken() {

        final DefaultTenantIdResolver sut = new DefaultTenantIdResolver(propertiesWithDefaultTenantId("default"));

        final OAuth2LoginAuthenticationToken token = mock(OAuth2LoginAuthenticationToken.class);
        final Optional<TenantId> actual = sut.resolve(token);

        assertThat(actual).contains(new TenantId("default"));
        verifyNoInteractions(token);
    }

    @Test
    void ensureResolveReturnsDefaultTenantIdForOidcUserAuthority() {

        final DefaultTenantIdResolver sut = new DefaultTenantIdResolver(propertiesWithDefaultTenantId("default"));

        final OidcUserAuthority authority = mock(OidcUserAuthority.class);
        final Optional<TenantId> actual = sut.resolve(authority);

        assertThat(actual).contains(new TenantId("default"));
        verifyNoInteractions(authority);
    }

    @Test
    void ensureResolveReturnsConfiguredCustomDefaultTenantId() {

        final DefaultTenantIdResolver sut = new DefaultTenantIdResolver(propertiesWithDefaultTenantId("acme"));

        assertThat(sut.resolve(mock(OAuth2AuthenticationToken.class))).contains(new TenantId("acme"));
        assertThat(sut.resolve(mock(OAuth2LoginAuthenticationToken.class))).contains(new TenantId("acme"));
        assertThat(sut.resolve(mock(OidcUserAuthority.class))).contains(new TenantId("acme"));
    }

    private static SingleTenantConfigurationProperties propertiesWithDefaultTenantId(String defaultTenantId) {
        final SingleTenantConfigurationProperties properties = new SingleTenantConfigurationProperties();
        properties.setDefaultTenantId(defaultTenantId);
        return properties;
    }
}
