package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;

@Component
class TenantUserCreatorAndUpdater {

    private final TenantUserService tenantUserService;

    TenantUserCreatorAndUpdater(TenantUserService tenantUserService) {
        this.tenantUserService = tenantUserService;
    }

    @EventListener
    public void handle(InteractiveAuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof final DefaultOidcUser oidcUser) {

            final EMailAddress eMailAddress = new EMailAddress(oidcUser.getEmail());
            final UserId userId = new UserId(oidcUser.getSubject());

            final Optional<TenantUser> maybeUser = tenantUserService.findById(userId);
            if (maybeUser.isPresent()) {
                final TenantUser user = maybeUser.get();
                final TenantUser tenantUser = new TenantUser(user.id(), user.localId(), oidcUser.getGivenName(), oidcUser.getFamilyName(), eMailAddress, user.authorities());
                tenantUserService.updateUser(tenantUser);
            } else {
                tenantUserService.createNewUser(oidcUser.getSubject(), oidcUser.getGivenName(), oidcUser.getFamilyName(), eMailAddress, Set.of(ZEITERFASSUNG_USER));
            }
        }
    }
}
