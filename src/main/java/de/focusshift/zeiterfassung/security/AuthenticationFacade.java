package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class AuthenticationFacade {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    /**
     * Returns the current {@link Authentication}.
     *
     * @return the current {@link Authentication}
     */
    public Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Returns the {@link UserIdComposite} of the current user.
     *
     * @return the {@link UserIdComposite} of the current user
     */
    public UserIdComposite getCurrentUserIdComposite() {
        final Authentication authentication = getCurrentAuthentication();
        return userIdCompositeFromAuth(authentication);
    }

    /**
     * Checks whether the current {@link Authentication} has the given {@link SecurityRole} or not.
     *
     * @param securityRole {@link SecurityRole} to check
     * @return <code>true</code> when the current authentication has the securityRole, <code>false</code> otherwise.
     */
    public boolean hasSecurityRole(SecurityRole securityRole) {

        final Authentication authentication = getCurrentAuthentication();
        final boolean hasRole = authentication.getAuthorities().contains(securityRole.authority());

        if (LOG.isDebugEnabled()) {
            final UserIdComposite userIdComposite = userIdCompositeFromAuth(authentication);
            LOG.debug("user={} has permission={}", userIdComposite.id(), hasRole);
        }

        return hasRole;
    }

    private static UserIdComposite userIdCompositeFromAuth(Authentication authentication) {

        if (authentication instanceof OAuth2AuthenticationToken token) {
            final OAuth2User oAuth2User = token.getPrincipal();
            if (oAuth2User instanceof CurrentOidcUser oidcUser) {

                final UserId userId = oidcUser.getUserId();

                final UserLocalId localId = oidcUser.getUserLocalId()
                    .orElseThrow(() -> new IllegalStateException("expected user local id to exist for " + userId));

                return new UserIdComposite(userId, localId);
            }
        }

        throw new IllegalStateException("unexpected authentication token: " + authentication.getPrincipal());
    }
}
