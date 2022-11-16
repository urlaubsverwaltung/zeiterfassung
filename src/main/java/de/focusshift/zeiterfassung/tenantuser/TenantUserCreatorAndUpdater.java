package de.focusshift.zeiterfassung.tenantuser;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class TenantUserCreatorAndUpdater {

    private final TenantUserService tenantUserService;

    TenantUserCreatorAndUpdater(TenantUserService tenantUserService) {
        this.tenantUserService = tenantUserService;
    }

    @EventListener
    public void handle(InteractiveAuthenticationSuccessEvent interactiveAuthenticationSuccessEvent) {
        if (interactiveAuthenticationSuccessEvent.getAuthentication().getPrincipal() instanceof final DefaultOidcUser oidcUser) {
            final UUID uuid = UUID.fromString(oidcUser.getSubject());
            final EMailAddress eMailAddress = new EMailAddress(oidcUser.getEmail());

            final Optional<TenantUser> maybeUser = tenantUserService.getUserByUuid(uuid);
            if (maybeUser.isEmpty()) {
                tenantUserService.createNewUser(uuid, oidcUser.getGivenName(), oidcUser.getFamilyName(), eMailAddress);
            } else {
                final TenantUser user = maybeUser.get();
                final TenantUser tenantUser = new TenantUser(user.id(), user.localId(), oidcUser.getGivenName(), oidcUser.getFamilyName(), eMailAddress);
                tenantUserService.updateUser(tenantUser);
            }
        }
    }
}
