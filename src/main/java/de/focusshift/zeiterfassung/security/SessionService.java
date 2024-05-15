package de.focusshift.zeiterfassung.security;


import de.focusshift.zeiterfassung.user.UserId;

public interface SessionService {

    /**
     * Mark the session of the given username to reload the authorities on the next page request
     *
     * @param userId to mark to reload authorities
     */
    void markSessionToReloadAuthorities(UserId userId);

    /**
     * Unmark the session to not reload the authorities.
     *
     * @param sessionId to unmark the session
     */
    void unmarkSessionToReloadAuthorities(String sessionId);
}
