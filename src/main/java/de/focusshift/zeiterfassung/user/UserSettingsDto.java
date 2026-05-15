package de.focusshift.zeiterfassung.user;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

public record UserSettingsDto(String theme, @Nullable Locale locale) {

    public UserSettingsDto(String theme) {
        this(theme, null);
    }
}
