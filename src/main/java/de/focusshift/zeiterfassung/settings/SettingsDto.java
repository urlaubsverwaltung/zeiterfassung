package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

record SettingsDto(
    @NotNull
    FederalState federalState,
    boolean worksOnPublicHoliday,
    boolean lockingIsActive,
    @Nullable
    @PositiveOrZero(message = "{settings.lock-timeentries-days-in-past.validation.positiveOrZero}")
    Integer lockTimeEntriesDaysInPast
) {
}
