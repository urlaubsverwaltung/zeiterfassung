package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.usermanagement.User;
import org.springframework.security.core.Authentication;

public interface CurrentUserProvider {

    Authentication getCurrentAuthentication();

    User getCurrentUser();
}
