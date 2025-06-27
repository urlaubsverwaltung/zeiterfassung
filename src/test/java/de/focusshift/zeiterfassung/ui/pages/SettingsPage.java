package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import de.focusshift.zeiterfassung.publicholiday.FederalState;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED;
import static de.focusshift.zeiterfassung.ui.pages.FederalStateSelect.federalStateSelectValue;

public class SettingsPage {

    private final Page page;

    public SettingsPage(Page page) {
        this.page = page;
    }

    public void selectFederalState(FederalState federalState) {
        executeUserAction(() -> {
            federalStateSelect().selectOption(federalStateSelectValue(federalState));
            federalStateSelect().click();
        });
    }

    public void assertFederalStateValue(FederalState federalState) {
        assertThat(federalStateSelect()).hasValue(federalStateSelectValue(federalState));
    }

    public void assertWorksOnPublicHolidayNotChecked() {
        assertThat(worksOnPublicHolidayCheckbox()).not().isChecked();
    }

    public void assertLockTimeEntriesNotChecked() {
        assertThat(lockTimeEntriesCheckbox()).not().isChecked();
    }

    public void enableLockTimeEntries() {
        executeUserAction(() -> {
            lockTimeEntriesCheckbox().check();
        });
    }

    public void setLockTimeEntriesDaysInPast(String daysInPast) {
        executeUserAction(() -> {
            lockTimEntriesDaysInPastInput().fill(daysInPast);
        });
    }

    public void submit() {
        final Locator submitButton = page.getByTestId("settings-submit-button");
        page.waitForResponse(Response::ok, submitButton::click);
        page.waitForLoadState(DOMCONTENTLOADED);
    }

    private void executeUserAction(Runnable runnable) {
        // updating interactive elements results in a "page-refresh" when javascript is enabled
        // 1. first response -> redirect
        // 2. second response -> ok with updated html
        // 3. js updates the turbo-frame, not the whole page
        page.waitForResponse(Response::ok, runnable);
    }

    private Locator worksOnPublicHolidayCheckbox() {
        return page.getByTestId("works-on-public-holiday-checkbox");
    }

    private Locator federalStateSelect() {
        return page.getByTestId("federal-state-select");
    }

    private Locator lockTimeEntriesCheckbox() {
        return page.getByTestId("locking-timeentries-is-active-checkbox");
    }

    private Locator lockTimEntriesDaysInPastInput() {
        return page.getByTestId("lock-timeentries-days-in-past");
    }
}
