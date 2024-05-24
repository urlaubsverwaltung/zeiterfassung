package de.focusshift.zeiterfassung.security;


import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

public interface SessionService {

    /**
     * Mark the session of the given username to reload the authorities on the next page request
     *
     * @param userId to mark to reload authorities
     */
    void markSessionToReloadAuthorities(UserId userId);

    /**
     * Mark the session of the given username to reload the authorities on the next page request
     *
     * @param userLocalId to mark to reload authorities
     */
    void markSessionToReloadAuthorities(UserLocalId userLocalId);

    /**
     * Unmark the session to not reload the authorities.
     *
     * @param sessionId to unmark the session
     */
    void unmarkSessionToReloadAuthorities(String sessionId);
}
