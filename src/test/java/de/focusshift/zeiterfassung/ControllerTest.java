package de.focusshift.zeiterfassung;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.security.core.GrantedAuthority;
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

        return oidcSubject(userId, userLocalId);
    }

    /**
     * Oidc Login using the given userId values.
     *
     * @param userId idToken / userInfoToken subject
     * @param userLocalId user local application id
     * @return {@link OidcLoginRequestPostProcessor} configured with idToken and userInfoToken
     */
    default OidcLoginRequestPostProcessor oidcSubject(UserId userId, UserLocalId userLocalId) {
        return oidcSubject(new UserIdComposite(userId, userLocalId));
    }

    /**
     * Oidc Login using the given subject.
     *
     * <p>
     * This user has no granted authorities!
     * Consider using {@link ControllerTest#oidcSubject(UserIdComposite, List)} if required.
     *
     * @param userIdComposite {@link UserIdComposite} of the current logged-in user
     * @return {@link OidcLoginRequestPostProcessor} configured with idToken and userInfoToken
     */
    default OidcLoginRequestPostProcessor oidcSubject(UserIdComposite userIdComposite) {
       return oidcSubject(userIdComposite, List.of());
    }

    /**
     * Oidc Login using the given subject and authorities.
     *
     * @param userIdComposite {@link UserIdComposite} of the current logged-in user
     * @param roles roles mapped to GrantedAuthority
     * @return {@link OidcLoginRequestPostProcessor} configured with idToken and userInfoToken
     */
    default OidcLoginRequestPostProcessor oidcSubject(UserIdComposite userIdComposite, List<SecurityRole> roles) {

        final List<GrantedAuthority> authorities =roles.stream().map(SecurityRole::authority).toList();

        final DefaultOidcUser defaultOidcUser = new DefaultOidcUser(authorities, OidcIdToken.withTokenValue("token-value").claim("sub", userIdComposite.id().value()).build());
        final CurrentOidcUser currentUser = new CurrentOidcUser(defaultOidcUser, List.of(), authorities, userIdComposite.localId());

        return oidcLogin().oidcUser(currentUser);
    }
}
