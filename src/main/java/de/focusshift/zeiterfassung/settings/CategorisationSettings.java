package de.focusshift.zeiterfassung.settings;

/**
 * Global settings controlling which categorisation fields are compulsory on time entries.
 *
 * @param projectRequired       when {@code true} users must select a project on every time entry
 * @param activityTypeRequired  when {@code true} users must select an activity type on every time entry
 */
public record CategorisationSettings(boolean projectRequired, boolean activityTypeRequired) {

    public static final CategorisationSettings DEFAULT = new CategorisationSettings(false, false);
}
