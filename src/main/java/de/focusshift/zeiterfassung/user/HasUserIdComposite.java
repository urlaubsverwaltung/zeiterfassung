package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

public interface HasUserIdComposite {

    UserIdComposite userIdComposite();

    default UserId userId() {
        return userIdComposite().id();
    }

    default UserLocalId userLocalId() {
        return userIdComposite().localId();
    }
}
