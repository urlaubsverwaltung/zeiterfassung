package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;

/**
 * Published when the global {@link FederalStateSettings} have been changed.
 *
 * <p>
 * Changing the global federal state or the {@code worksOnPublicHoliday} flag alters the public holidays (and therefore
 * the planned working hours) of every person whose {@link de.focusshift.zeiterfassung.workingtime.WorkingTime} inherits
 * the global setting. The old and new values are carried so consumers can determine exactly which public holidays are
 * affected (old ∪ new federal state).
 *
 * @param oldFederalState federal state before the update
 * @param oldWorksOnPublicHoliday whether persons worked on public holidays before the update
 * @param newFederalState federal state after the update
 * @param newWorksOnPublicHoliday whether persons work on public holidays after the update
 */
public record FederalStateSettingsUpdatedEvent(
    FederalState oldFederalState,
    boolean oldWorksOnPublicHoliday,
    FederalState newFederalState,
    boolean newWorksOnPublicHoliday
) {
}
