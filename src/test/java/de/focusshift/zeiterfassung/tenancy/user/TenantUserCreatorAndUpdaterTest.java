package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
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

        final Map<String, Object> sub = Map.of(
            "sub", "uniqueIdentifier",
            "given_name", "Samuel",
            "family_name", "Jackson",
            "email", "s.jackson@example.org"
        );
        final OidcIdToken idToken = new OidcIdToken("tokenValue", Instant.now(), Instant.MAX, sub);


        final List<GrantedAuthority> grantedAuthorities = List.of();
        final Authentication authentication = new OAuth2AuthenticationToken(new DefaultOidcUser(grantedAuthorities, idToken), grantedAuthorities, "myRegistrationId");
        final InteractiveAuthenticationSuccessEvent event = new InteractiveAuthenticationSuccessEvent(authentication, this.getClass());

        final TenantUser tenantUser = new TenantUser("uniqueIdentifier", 1L, "Samuel", "Jackson", new EMailAddress("s.jackson@example.org"), Set.of());
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
        final Authentication authentication = new OAuth2AuthenticationToken(new DefaultOidcUser(grantedAuthorities, idToken), grantedAuthorities, "myRegistrationId");
        final InteractiveAuthenticationSuccessEvent event = new InteractiveAuthenticationSuccessEvent(authentication, this.getClass());

        when(tenantUserService.findById(new UserId("uniqueIdentifier"))).thenReturn(Optional.empty());

        sut.handle(event);

        verify(tenantUserService).createNewUser("uniqueIdentifier", "Samuel", "Jackson", new EMailAddress("s.jackson@example.org"), Set.of(ZEITERFASSUNG_USER));

        InOrder inOrder = Mockito.inOrder(tenantContextHolder);
        inOrder.verify(tenantContextHolder).setTenantId(new TenantId("myRegistrationId"));
        inOrder.verify(tenantContextHolder).clear();
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
}
