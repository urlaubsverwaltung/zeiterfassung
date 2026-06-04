package de.focusshift.zeiterfassung.settings;

public interface CategorisationSettingsService {

    /**
     * Returns the persisted {@link CategorisationSettings}, or the defaults when nothing has been
     * configured yet. Never {@code null}.
     */
    CategorisationSettings getCategorisationSettings();
}
