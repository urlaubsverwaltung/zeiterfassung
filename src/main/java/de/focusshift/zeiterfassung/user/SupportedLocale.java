package de.focusshift.zeiterfassung.user;

import java.util.Locale;

enum SupportedLocale {

    GERMAN(Locale.GERMAN),
    ENGLISH(Locale.ENGLISH);

    private final Locale locale;

    SupportedLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }
}
