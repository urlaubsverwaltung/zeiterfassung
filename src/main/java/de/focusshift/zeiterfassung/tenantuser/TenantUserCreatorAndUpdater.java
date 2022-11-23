package de.focusshift.zeiterfassung.tenantuser;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

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

            final Set<SecurityRoles> authorities = oidcUser.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(startsWith("ROLE_").and(not("ROLE_USER"::equals)))
                .map(s -> s.substring("ROLE_".length()))
                .map(SecurityRoles::valueOf)
                .collect(toSet());

            final Optional<TenantUser> maybeUser = tenantUserService.getUserByUuid(uuid);
            if (maybeUser.isEmpty()) {
                tenantUserService.createNewUser(uuid, oidcUser.getGivenName(), oidcUser.getFamilyName(), eMailAddress, authorities);
            } else {
                final TenantUser user = maybeUser.get();
                final TenantUser tenantUser = new TenantUser(user.id(), user.localId(), oidcUser.getGivenName(), oidcUser.getFamilyName(), eMailAddress, authorities);
                tenantUserService.updateUser(tenantUser);
            }
        }
    }

    private static Predicate<String> startsWith(String prefix) {
        return s -> s.startsWith(prefix);
    }
}
