package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationFacadeTest {

    private AuthenticationFacade sut;

    @BeforeEach
    void setUp() {
        sut = new AuthenticationFacade();
    }

    @Nested
    class GetCurrentAuthentication {

        @Test
        void ensureGetCurrentAuthenticationReturnsSecurityContextAuthentication() throws Exception {
            final Authentication authentication = mock(Authentication.class);
            withMockedAuthentication(authentication, () -> {
                final Authentication actual = sut.getCurrentAuthentication();
                assertThat(actual).isSameAs(authentication);
            });
        }
    }

    @Nested
    class GetCurrentUserIdComposite {

        @Test
        void ensureGetCurrentUserIdCompositeThrowsWhenAuthenticationNotInstanceOfOAuthToken() throws Exception {
            final Authentication authentication = new UsernamePasswordAuthenticationToken("username", "password");

            withMockedAuthentication(authentication, () -> {
                assertThatThrownBy(() -> sut.getCurrentUserIdComposite())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("unexpected authentication token: username");
            });
        }

        @Test
        void ensureGetCurrentUserIdCompositeThrowsWhenAuthenticationDoesNotContainCurrentOidcUser() throws Exception {
            final OidcUser user = new DefaultOidcUser(List.of(), OidcIdToken.withTokenValue("token-value").subject("subject").build());
            final Authentication authentication = new OAuth2AuthenticationToken(user, List.of(), "client-id");

            withMockedAuthentication(authentication, () -> {
                assertThatThrownBy(() -> sut.getCurrentUserIdComposite())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("unexpected authentication token: Name: [subject], Granted Authorities: [[]], User Attributes: [{sub=subject}]");
            });
        }

        @Test
        void ensureGetCurrentUserIdCompositeThrowsWhenUserHasNoUserLocalId() throws Exception {
            final OidcUser oidcUser = new DefaultOidcUser(List.of(), OidcIdToken.withTokenValue("token-value").subject("subject").build());
            final OidcUser user = new CurrentOidcUser(oidcUser, List.of(), List.of(), null);
            final Authentication authentication = new OAuth2AuthenticationToken(user, List.of(), "client-id");

            withMockedAuthentication(authentication, () -> {
                assertThatThrownBy(() -> sut.getCurrentUserIdComposite())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("expected user local id to exist for UserId[value=subject]");
            });
        }

        @Test
        void ensureGetCurrentUserIdComposite() throws Exception {

            final UserId userId = new UserId("batman");
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

            final OidcUser oidcUser = new DefaultOidcUser(List.of(), OidcIdToken.withTokenValue("token-value").subject(userId.value()).build());
            final OidcUser user = new CurrentOidcUser(oidcUser, List.of(), List.of(), userLocalId);
            final Authentication authentication = new OAuth2AuthenticationToken(user, List.of(), "client-id");

            withMockedAuthentication(authentication, () -> {
                final UserIdComposite actual = sut.getCurrentUserIdComposite();
                assertThat(actual).isEqualTo(userIdComposite);
            });
        }
    }

    @Nested
    class HasSecurityRole {

        @Test
        void ensureHasSecurityRoleReturnsFalse() throws Exception {

            final UserId userId = new UserId("batman");
            final UserLocalId userLocalId = new UserLocalId(1L);

            final OidcUser oidcUser = new DefaultOidcUser(List.of(), OidcIdToken.withTokenValue("token-value").subject(userId.value()).build());
            final OidcUser user = new CurrentOidcUser(oidcUser, List.of(), List.of(), userLocalId);
            final Authentication authentication = new OAuth2AuthenticationToken(user, List.of(), "client-id");

            withMockedAuthentication(authentication, () -> {
                final boolean actual = sut.hasSecurityRole(SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL);
                assertThat(actual).isFalse();
            });
        }

        @Test
        void ensureHasSecurityRoleReturnsTrue() throws Exception {

            final UserId userId = new UserId("batman");
            final UserLocalId userLocalId = new UserLocalId(1L);

            final OidcIdToken oidcToken = OidcIdToken.withTokenValue("token-value").subject(userId.value()).build();
            final OidcUser oidcUser = new DefaultOidcUser(List.of(), oidcToken);
            final OidcUser user = new CurrentOidcUser(oidcUser, List.of(), List.of(), userLocalId);
            final Authentication authentication = new OAuth2AuthenticationToken(user, List.of(new SimpleGrantedAuthority("ZEITERFASSUNG_VIEW_REPORT_ALL")), "client-id");

            withMockedAuthentication(authentication, () -> {
                final boolean actual = sut.hasSecurityRole(SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL);
                assertThat(actual).isTrue();
            });
        }
    }

    private void withMockedAuthentication(Authentication authentication, AssertionCallback runnable) throws Exception {

        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> securityContextMock = mockStatic(SecurityContextHolder.class)) {
            securityContextMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            runnable.call();
        }
    }

    @FunctionalInterface
    interface AssertionCallback {
        void call() throws Exception;
    }
}
