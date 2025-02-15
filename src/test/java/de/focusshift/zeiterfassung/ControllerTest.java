package de.focusshift.zeiterfassung;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

public interface ControllerTest {

    /**
     * Oidc Login using the given subject.
     *
     * @param subject idToken / userInfoToken subject
     * @return {@link OidcLoginRequestPostProcessor} configured with idToken and userInfoToken
     */
    default OidcLoginRequestPostProcessor oidcSubject(String subject) {
        return oidcLogin()
            .idToken(id -> id.subject(subject))
            .userInfoToken(userInfo -> userInfo.subject(subject));
    }
}
