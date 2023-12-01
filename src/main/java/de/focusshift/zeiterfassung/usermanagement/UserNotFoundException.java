package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;

/**
 * Thrown when a user could not be found. This exception should be handled in a {@linkplain org.springframework.web.bind.annotation.ControllerAdvice}
 * or {@linkplain org.springframework.web.bind.annotation.RestControllerAdvice}.
 */
public class UserNotFoundException extends Exception {

    public UserNotFoundException(UserIdComposite userIdComposite) {
        super("could not find user=%s".formatted(userIdComposite));
    }

    public UserNotFoundException(UserLocalId userLocalId) {
        super("could not find user=%s".formatted(userLocalId));
    }

    public UserNotFoundException(UserId userId) {
        super("could not find user=%s".formatted(userId));
    }
}
