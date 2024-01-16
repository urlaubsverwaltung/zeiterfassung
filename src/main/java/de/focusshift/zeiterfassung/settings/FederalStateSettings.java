package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;

/**
 * Global federal-state settings. Can be overridden for an individual person.
 *
 * @param federalState the default federal-state and public holiday regulations
 * @param worksOnPublicHoliday whether persons are working on public holidays or not
 */
public record FederalStateSettings(FederalState federalState, boolean worksOnPublicHoliday) {

    public static final FederalStateSettings DEFAULT = new FederalStateSettings(FederalState.NONE, false);
}
