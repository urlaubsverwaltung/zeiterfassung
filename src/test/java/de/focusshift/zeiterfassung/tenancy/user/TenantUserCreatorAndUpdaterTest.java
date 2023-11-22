package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantUserCreatorAndUpdaterTest {

    @InjectMocks
    private TenantUserCreatorAndUpdater sut;

    @Mock
    private TenantUserService tenantUserService;

    @Test
    void ensureToUpdateTenantUserIfTenantUserWithSubjectDoesExist() {

        final Authentication authentication = mock(Authentication.class);

        final Map<String, Object> sub = Map.of(
            "sub", "uniqueIdentifier",
            "given_name", "Samuel",
            "family_name", "Jackson",
            "email", "s.jackson@example.org"
        );
        final OidcIdToken idToken = new OidcIdToken("tokenValue", Instant.now(), Instant.MAX, sub);
        when(authentication.getPrincipal()).thenReturn(new DefaultOidcUser(List.of(), idToken));

        final InteractiveAuthenticationSuccessEvent event = new InteractiveAuthenticationSuccessEvent(authentication, this.getClass());

        final TenantUser tenantUser = new TenantUser("uniqueIdentifier", 1L, "Samuel", "Jackson", new EMailAddress("s.jackson@example.org"), Set.of());
        when(tenantUserService.findById(new UserId("uniqueIdentifier"))).thenReturn(Optional.of(tenantUser));

        sut.handle(event);

        verify(tenantUserService).updateUser(tenantUser);
    }


    @Test
    void ensureToCreateNewTenantUserIfTenantUserWithSubjectDoesNotExist() {

        final Authentication authentication = mock(Authentication.class);

        final Map<String, Object> sub = Map.of(
            "sub", "uniqueIdentifier",
            "given_name", "Samuel",
            "family_name", "Jackson",
            "email", "s.jackson@example.org"
        );
        final OidcIdToken idToken = new OidcIdToken("tokenValue", Instant.now(), Instant.MAX, sub);
        when(authentication.getPrincipal()).thenReturn(new DefaultOidcUser(List.of(), idToken));

        final InteractiveAuthenticationSuccessEvent event = new InteractiveAuthenticationSuccessEvent(authentication, this.getClass());

        when(tenantUserService.findById(new UserId("uniqueIdentifier"))).thenReturn(Optional.empty());

        sut.handle(event);

        verify(tenantUserService).createNewUser("uniqueIdentifier", "Samuel", "Jackson", new EMailAddress("s.jackson@example.org"), Set.of(ZEITERFASSUNG_USER));
    }
}
