package de.focusshift.zeiterfassung.user;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public class UserSettings {

    private final Theme theme;
    private final Locale locale;
    private final Locale localeBrowserSpecific;
    @Nullable
    private final String githubLogin;
    private final boolean githubLoginVerified;
    @Nullable
    private final String githubToken;
    private final boolean notificationsEnabled;
    @Nullable
    private final Long githubInstallationId;

    UserSettings(Theme theme, Locale locale, Locale localeBrowserSpecific,
                 @Nullable String githubLogin, boolean githubLoginVerified,
                 @Nullable String githubToken, boolean notificationsEnabled,
                 @Nullable Long githubInstallationId) {
        this.theme = theme;
        this.locale = locale;
        this.localeBrowserSpecific = localeBrowserSpecific;
        this.githubLogin = githubLogin;
        this.githubLoginVerified = githubLoginVerified;
        this.githubToken = githubToken;
        this.notificationsEnabled = notificationsEnabled;
        this.githubInstallationId = githubInstallationId;
    }

    public Theme theme() {
        return theme;
    }

    public Optional<Locale> locale() {
        return Optional.ofNullable(locale);
    }

    public Optional<Locale> localeBrowserSpecific() {
        return Optional.ofNullable(localeBrowserSpecific);
    }

    public Optional<String> githubLogin() {
        return Optional.ofNullable(githubLogin);
    }

    public boolean githubLoginVerified() {
        return githubLoginVerified;
    }

    public Optional<String> githubToken() {
        return Optional.ofNullable(githubToken);
    }

    public boolean notificationsEnabled() {
        return notificationsEnabled;
    }

    /**
     * Returns the GitHub App installation ID for the user's personal account,
     * or empty if no personal installation has been connected.
     */
    public Optional<Long> githubInstallationId() {
        return Optional.ofNullable(githubInstallationId);
    }
}
