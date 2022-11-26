package de.focusshift.zeiterfassung.security;


public interface SessionService {

    String RELOAD_AUTHORITIES = "reloadAuthorities";

    /**
     * Mark the session of the given username to reload the authorities on the next page request
     *
     * @param username to mark to reload authorities
     */
    void markSessionToReloadAuthorities(String username);

    /**
     * Unmark the session to not reload the authorities.
     *
     * @param sessionId to unmark the session
     */
    void unmarkSessionToReloadAuthorities(String sessionId);
}
