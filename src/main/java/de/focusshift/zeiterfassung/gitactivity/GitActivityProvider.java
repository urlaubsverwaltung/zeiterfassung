package de.focusshift.zeiterfassung.gitactivity;

import java.time.Instant;
import java.util.List;

public interface GitActivityProvider {

    /** Short platform identifier, e.g. "GITHUB", "GITLAB", "BITBUCKET". */
    String platform();

    boolean isConfigured();

    /** Human-readable description of which config properties are still missing. */
    String missingConfig();

    /** Returns all platform usernames that should be synced (e.g. all verified GitHub logins). */
    List<String> resolveUsernames();

    /** Fetches and persists new activity events for the given platform username. */
    void syncUser(String platformUsername);

    /** Returns the timestamp of the last successful sync for {@code platformUsername}, or null. */
    Instant getLastSyncTime(String platformUsername);
}
