package de.focusshift.zeiterfassung.settings;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum SupportedLanguages {

    // order is important as it represents the order in UI for instance

    GERMAN(Locale.GERMAN),
    ENGLISH(Locale.ENGLISH);

    private final Locale locale;

    SupportedLanguages(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the SupportedLanguage with the specified locale.
     *
     * @return the SupportedLanguage with the specified locale
     * @throws IllegalArgumentException if there is no SupportedLanguage with the specified locale
     */
    public static Optional<SupportedLanguages> valueOfLocale(Locale locale) {
        return Arrays.stream(SupportedLanguages.values())
            .filter(supportedLanguages -> supportedLanguages.locale.equals(locale))
            .findFirst();
    }
}
