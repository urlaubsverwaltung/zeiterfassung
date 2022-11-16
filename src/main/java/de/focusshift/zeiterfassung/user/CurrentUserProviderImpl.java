package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
class CurrentUserProviderImpl implements CurrentUserProvider {

    private final UserManagementService userManagementService;

    CurrentUserProviderImpl(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @Override
    public Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public User getCurrentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof OAuth2AuthenticationToken token) {
            final OAuth2User oAuth2User = token.getPrincipal();
            if (oAuth2User instanceof OidcUser oidcUser) {
                return userManagementService.findUserById(new UserId(oidcUser.getSubject()))
                    .orElseThrow(() -> new IllegalStateException(String.format("could not find logged-in user with id=%s in database", oidcUser.getSubject())));
            }
        }

        throw new IllegalStateException("could not get current user due to unsupported authentication mechanism.");
    }
}
