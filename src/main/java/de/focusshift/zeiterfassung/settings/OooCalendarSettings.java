package de.focusshift.zeiterfassung.settings;

import jakarta.annotation.Nullable;

/**
 * Settings for the Urlaubsverwaltung (ooo) iCal calendar integration.
 *
 * @param calendarUrl the company iCal feed URL including the secret token, or {@code null} if not configured
 */
public record OooCalendarSettings(@Nullable String calendarUrl) {

    public static final OooCalendarSettings DEFAULT = new OooCalendarSettings(null);

    public boolean isConfigured() {
        return calendarUrl != null && !calendarUrl.isBlank();
    }
}
