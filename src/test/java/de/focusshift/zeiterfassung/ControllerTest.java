package de.focusshift.zeiterfassung;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

public interface ControllerTest {

    /**
     * Oidc Login using the given subject.
     *
     * @param subject idToken / userInfoToken subject
     * @return {@link OidcLoginRequestPostProcessor} configured with idToken and userInfoToken
     */
    default OidcLoginRequestPostProcessor oidcSubject(String subject) {

        final UserId userId = new UserId(subject);
        final UserLocalId userLocalId = new UserLocalId(1L);

        return oidcSubject(new UserIdComposite(userId, userLocalId));
    }

    /**
     * Oidc Login using the given subject.
     *
     * @param userIdComposite {@link UserIdComposite} of the current logged-in user
     * @return {@link OidcLoginRequestPostProcessor} configured with idToken and userInfoToken
     */
    default OidcLoginRequestPostProcessor oidcSubject(UserIdComposite userIdComposite) {

        final DefaultOidcUser defaultOidcUser = new DefaultOidcUser(List.of(), OidcIdToken.withTokenValue("token-value").claim("sub", userIdComposite.id().value()).build());
        final CurrentOidcUser currentUser = new CurrentOidcUser(defaultOidcUser, List.of(), List.of(), userIdComposite.localId());

        return oidcLogin().oidcUser(currentUser);
    }
}
