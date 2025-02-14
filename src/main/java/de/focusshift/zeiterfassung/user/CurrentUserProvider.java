package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.usermanagement.User;

public interface CurrentUserProvider {

    User getCurrentUser();
}
