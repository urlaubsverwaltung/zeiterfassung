package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.user.UserId;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class AuthenticationService {

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
     * Returns the {@link UserId} of the current {@link Authentication}.
     *
     * @return the {@link UserId} of the current {@link Authentication}
     * @throws IllegalStateException when authentication is of unexpected type, e.g. not OAuth.
     */
    public UserId getCurrentUserId() {

        final Authentication authentication = getCurrentAuthentication();

        if (authentication instanceof OAuth2AuthenticationToken token) {
            final OAuth2User oAuth2User = token.getPrincipal();
            if (oAuth2User instanceof OidcUser oidcUser) {
                return new UserId(oidcUser.getSubject());
            }
        }

        throw new IllegalStateException("unexpected authentication token: " + authentication.getPrincipal());
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
            LOG.debug("user={} has permission={}", authentication.getName(), hasRole);
        }

        return hasRole;
    }
}
