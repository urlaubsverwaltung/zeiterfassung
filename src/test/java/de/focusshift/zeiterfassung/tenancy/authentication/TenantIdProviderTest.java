package de.focusshift.zeiterfassung.tenancy.authentication;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantIdProviderTest {

    @Nested
    class ResolveOAuth2AuthenticationToken {

        @Mock
        private OAuth2AuthenticationToken token;

        @Test
        void ensureFirstNonEmptyWinsAndSubsequentResolversAreNotConsulted() {

            final TenantIdResolver r1 = mock(TenantIdResolver.class);
            final TenantIdResolver r2 = mock(TenantIdResolver.class);
            final TenantIdResolver r3 = mock(TenantIdResolver.class);

            when(r1.resolve(token)).thenReturn(Optional.empty());
            when(r2.resolve(token)).thenReturn(Optional.of(new TenantId("t2")));

            final TenantIdProvider sut = new TenantIdProvider(List.of(r1, r2, r3));

            final Optional<TenantId> actual = sut.resolve(token);

            assertThat(actual).hasValue(new TenantId("t2"));
            verify(r3, never()).resolve(token);
        }

        @Test
        void ensureEmptyWhenNoResolverMatches() {

            final TenantIdResolver r1 = mock(TenantIdResolver.class);
            final TenantIdResolver r2 = mock(TenantIdResolver.class);

            when(r1.resolve(token)).thenReturn(Optional.empty());
            when(r2.resolve(token)).thenReturn(Optional.empty());

            final TenantIdProvider sut = new TenantIdProvider(List.of(r1, r2));

            assertThat(sut.resolve(token)).isEmpty();
        }

        @Test
        void ensureEmptyForEmptyResolverList() {

            final TenantIdProvider sut = new TenantIdProvider(List.of());

            assertThat(sut.resolve(token)).isEmpty();
        }
    }

    @Nested
    class ResolveOAuth2LoginAuthenticationToken {

        @Mock
        private OAuth2LoginAuthenticationToken token;

        @Test
        void ensureFirstNonEmptyWinsAndSubsequentResolversAreNotConsulted() {

            final TenantIdResolver r1 = mock(TenantIdResolver.class);
            final TenantIdResolver r2 = mock(TenantIdResolver.class);
            final TenantIdResolver r3 = mock(TenantIdResolver.class);

            when(r1.resolve(token)).thenReturn(Optional.empty());
            when(r2.resolve(token)).thenReturn(Optional.of(new TenantId("t2")));

            final TenantIdProvider sut = new TenantIdProvider(List.of(r1, r2, r3));

            final Optional<TenantId> actual = sut.resolve(token);

            assertThat(actual).hasValue(new TenantId("t2"));
            verify(r3, never()).resolve(token);
        }

        @Test
        void ensureEmptyWhenNoResolverMatches() {

            final TenantIdResolver r1 = mock(TenantIdResolver.class);
            final TenantIdResolver r2 = mock(TenantIdResolver.class);

            when(r1.resolve(token)).thenReturn(Optional.empty());
            when(r2.resolve(token)).thenReturn(Optional.empty());

            final TenantIdProvider sut = new TenantIdProvider(List.of(r1, r2));

            assertThat(sut.resolve(token)).isEmpty();
        }

        @Test
        void ensureEmptyForEmptyResolverList() {

            final TenantIdProvider sut = new TenantIdProvider(List.of());

            assertThat(sut.resolve(token)).isEmpty();
        }
    }

    @Nested
    class ResolveOidcUserAuthority {

        @Mock
        private OidcUserAuthority authority;

        @Test
        void ensureFirstNonEmptyWinsAndSubsequentResolversAreNotConsulted() {

            final TenantIdResolver r1 = mock(TenantIdResolver.class);
            final TenantIdResolver r2 = mock(TenantIdResolver.class);
            final TenantIdResolver r3 = mock(TenantIdResolver.class);

            when(r1.resolve(authority)).thenReturn(Optional.empty());
            when(r2.resolve(authority)).thenReturn(Optional.of(new TenantId("t2")));

            final TenantIdProvider sut = new TenantIdProvider(List.of(r1, r2, r3));

            final Optional<TenantId> actual = sut.resolve(authority);

            assertThat(actual).hasValue(new TenantId("t2"));
            verify(r3, never()).resolve(authority);
        }

        @Test
        void ensureEmptyWhenNoResolverMatches() {

            final TenantIdResolver r1 = mock(TenantIdResolver.class);
            final TenantIdResolver r2 = mock(TenantIdResolver.class);

            when(r1.resolve(authority)).thenReturn(Optional.empty());
            when(r2.resolve(authority)).thenReturn(Optional.empty());

            final TenantIdProvider sut = new TenantIdProvider(List.of(r1, r2));

            assertThat(sut.resolve(authority)).isEmpty();
        }

        @Test
        void ensureEmptyForEmptyResolverList() {

            final TenantIdProvider sut = new TenantIdProvider(List.of());

            assertThat(sut.resolve(authority)).isEmpty();
        }
    }
}
