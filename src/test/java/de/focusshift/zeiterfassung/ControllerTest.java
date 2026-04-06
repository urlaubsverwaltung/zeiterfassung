package de.focusshift.zeiterfassung;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;

import java.util.List;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

public interface ControllerTest {

    default UserIdComposite anyUserIdComposite() {
        return anyUserIdComposite(new UserId("uuid"));
    }

    default UserIdComposite anyUserIdComposite(UserId userId) {
        return new UserIdComposite(userId, new UserLocalId(1L));
    }

    default User anyUser(UserIdComposite userIdComposite) {
        return new User(userIdComposite, "given name", "family name", new EMailAddress("email@example.org"), Set.of());
    }

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
     * @param userId      idToken / userInfoToken subject
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
     * @param roles           roles mapped to GrantedAuthority
     * @return {@link OidcLoginRequestPostProcessor} configured with idToken and userInfoToken
     */
    default OidcLoginRequestPostProcessor oidcSubject(UserIdComposite userIdComposite, List<SecurityRole> roles) {
        final CurrentOidcUser currentOidcUser = currentOidcUser(userIdComposite, roles);
        return oidcLogin().oidcUser(currentOidcUser);
    }

    default CurrentOidcUser currentOidcUser(UserIdComposite userIdComposite, List<SecurityRole> roles) {

        final List<GrantedAuthority> authorities = roles.stream().map(SecurityRole::authority).toList();

        final OidcIdToken.Builder tokenBuilder = OidcIdToken.withTokenValue("token-value")
            .claim("sub", userIdComposite.id().value());

        final OidcUserInfo.Builder userInfoBuilder = OidcUserInfo.builder()
            .subject(userIdComposite.id().value())
            .name("Some Name");

        final DefaultOidcUser defaultOidcUser = new DefaultOidcUser(authorities, tokenBuilder.build(), userInfoBuilder.build());
        return new CurrentOidcUser(defaultOidcUser, List.of(), authorities, userIdComposite.localId());
    }
}
