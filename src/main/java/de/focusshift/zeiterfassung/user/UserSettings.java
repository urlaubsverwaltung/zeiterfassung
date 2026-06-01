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

    UserSettings(Theme theme, Locale locale, Locale localeBrowserSpecific, @Nullable String githubLogin) {
        this.theme = theme;
        this.locale = locale;
        this.localeBrowserSpecific = localeBrowserSpecific;
        this.githubLogin = githubLogin;
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
}
