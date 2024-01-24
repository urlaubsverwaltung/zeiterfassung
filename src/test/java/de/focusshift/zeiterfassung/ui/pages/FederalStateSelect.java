package de.focusshift.zeiterfassung.ui.pages;

import de.focusshift.zeiterfassung.publicholiday.FederalState;

public class FederalStateSelect {

    private FederalStateSelect() {
        //
    }

    /**
     * Returns the {@code select option} value for the federalState.
     * Useful in conjunction with {@linkplain com.microsoft.playwright.assertions.PlaywrightAssertions}.
     */
    public static String federalStateSelectValue(FederalState federalState) {
        return federalState.name();
    }
}
