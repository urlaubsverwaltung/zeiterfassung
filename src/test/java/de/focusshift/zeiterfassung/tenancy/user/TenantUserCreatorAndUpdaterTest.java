package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantUserCreatorAndUpdaterTest {

    @InjectMocks
    private TenantUserCreatorAndUpdater sut;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TenantContextHolder tenantContextHolder;
    @Mock
    private TenantUserService tenantUserService;

    @Test
    void ensureToUpdateTenantUserIfTenantUserWithSubjectDoesExist() {

        final TenantUser tenantUser = anyTenantUser("uniqueIdentifier", Set.of());

        final Map<String, Object> sub = Map.of(
            "sub", tenantUser.id(),
            "given_name", tenantUser.givenName(),
            "family_name", tenantUser.familyName(),
            "email", tenantUser.eMail().value()
        );
        final OidcIdToken idToken = new OidcIdToken("tokenValue", Instant.now(), Instant.MAX, sub);

        final List<GrantedAuthority> grantedAuthorities = List.of();
        final DefaultOidcUser oidcUser = new DefaultOidcUser(grantedAuthorities, idToken);
        final CurrentOidcUser currentOidcUser = new CurrentOidcUser(oidcUser, List.of(), grantedAuthorities, new UserLocalId(tenantUser.localId()));

        final Authentication authentication = new OAuth2AuthenticationToken(currentOidcUser, grantedAuthorities, "myRegistrationId");
        final InteractiveAuthenticationSuccessEvent event = new InteractiveAuthenticationSuccessEvent(authentication, this.getClass());


        when(tenantUserService.findById(new UserId("uniqueIdentifier"))).thenReturn(Optional.of(tenantUser));

        sut.handle(event);

        verify(tenantUserService).updateUser(tenantUser);

        InOrder inOrder = Mockito.inOrder(tenantContextHolder);
        inOrder.verify(tenantContextHolder).setTenantId(new TenantId("myRegistrationId"));
        inOrder.verify(tenantContextHolder).clear();
    }


    @Test
    void ensureToCreateNewTenantUserIfTenantUserWithSubjectDoesNotExist() {

        final Map<String, Object> sub = Map.of(
            "sub", "uniqueIdentifier",
            "given_name", "Samuel",
            "family_name", "Jackson",
            "email", "s.jackson@example.org"
        );
        final OidcIdToken idToken = new OidcIdToken("tokenValue", Instant.now(), Instant.MAX, sub);

        final List<GrantedAuthority> grantedAuthorities = List.of();
        final DefaultOidcUser oidcUser = new DefaultOidcUser(grantedAuthorities, idToken);
        final CurrentOidcUser currentOidcUser = new CurrentOidcUser(oidcUser, List.of(), grantedAuthorities);

        final Authentication authentication = new OAuth2AuthenticationToken(currentOidcUser, grantedAuthorities, "myRegistrationId");
        final InteractiveAuthenticationSuccessEvent event = new InteractiveAuthenticationSuccessEvent(authentication, this.getClass());

        // user does not exist yet
        when(tenantUserService.findById(new UserId("uniqueIdentifier"))).thenReturn(Optional.empty());

        // and will be created
        final TenantUser createdTenantUser = new TenantUser("uniqueIdentifier", 1L, "Samuel", "Jackson",
            new EMailAddress("s.jackson@example.org"), Instant.now(), Set.of(ZEITERFASSUNG_USER),
            Instant.now(), Instant.now(), Instant.now(), Instant.now(), UserStatus.ACTIVE);
        when(tenantUserService.createNewUser("uniqueIdentifier", "Samuel", "Jackson", new EMailAddress("s.jackson@example.org"), Set.of(ZEITERFASSUNG_USER)))
            .thenReturn(createdTenantUser);

        sut.handle(event);

        final InOrder tenantContextInOrder = Mockito.inOrder(tenantContextHolder);
        tenantContextInOrder.verify(tenantContextHolder).setTenantId(new TenantId("myRegistrationId"));
        tenantContextInOrder.verify(tenantContextHolder).clear();

        final Authentication updatedAuthentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(updatedAuthentication.getPrincipal()).isInstanceOf(CurrentOidcUser.class);
        assertThat(updatedAuthentication.getPrincipal()).satisfies(principal -> {
            final CurrentOidcUser actualCurrentOidcUser = (CurrentOidcUser) principal;
            assertThat(actualCurrentOidcUser.getUserId()).isEqualTo(new UserId("uniqueIdentifier"));
            assertThat(actualCurrentOidcUser.getUserLocalId()).hasValue(new UserLocalId(1L));
            assertThat(actualCurrentOidcUser.getOidcUser()).isSameAs(currentOidcUser);
        });
    }

    @Test
    void ensureNoTenantUserServiceInteractionForInvalidTenantId() {

        final Map<String, Object> sub = Map.of(
            "sub", "uniqueIdentifier",
            "given_name", "Samuel",
            "family_name", "Jackson",
            "email", "s.jackson@example.org"
        );
        final OidcIdToken idToken = new OidcIdToken("tokenValue", Instant.now(), Instant.MAX, sub);

        final List<GrantedAuthority> grantedAuthorities = List.of();

        // pass invalid clientRegistrationId, so that TenantId is invalid
        final Authentication authentication = new OAuth2AuthenticationTokenWithoutClientRegistrationId(new DefaultOidcUser(grantedAuthorities, idToken), grantedAuthorities, "invalid");

        final InteractiveAuthenticationSuccessEvent event = new InteractiveAuthenticationSuccessEvent(authentication, this.getClass());

        sut.handle(event);

        verifyNoInteractions(tenantUserService);

        verify(tenantContextHolder, never()).setTenantId(any());
        verify(tenantContextHolder, never()).clear();
    }

    private static class OAuth2AuthenticationTokenWithoutClientRegistrationId extends OAuth2AuthenticationToken {
        public OAuth2AuthenticationTokenWithoutClientRegistrationId(OAuth2User principal, Collection<? extends GrantedAuthority> authorities, String authorizedClientRegistrationId) {
            super(principal, authorities, authorizedClientRegistrationId);
        }

        public String getAuthorizedClientRegistrationId() {
            // yes - this is an ugly solution, but it is an easy way to test the invalid tenantId case
            return "";
        }
    }

    private TenantUser anyTenantUser(String id, Set<SecurityRole> authorities) {
        Instant now = Instant.now();
        return new TenantUser(id, 1L, "Bruce", "Wayne", new EMailAddress("batman@example.org"), now, authorities, now, now, null, null, UserStatus.ACTIVE);
    }
}
