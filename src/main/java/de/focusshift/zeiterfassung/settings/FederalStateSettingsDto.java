package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;

record FederalStateSettingsDto(FederalState federalState, boolean worksOnPublicHoliday) {
}
