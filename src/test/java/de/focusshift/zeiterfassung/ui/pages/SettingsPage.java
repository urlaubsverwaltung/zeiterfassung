package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import de.focusshift.zeiterfassung.publicholiday.FederalState;

import java.time.DayOfWeek;

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

    public void assertLockTimeEntriesDaysInPastInputValue(String expectedValue) {
        assertThat(lockTimEntriesDaysInPastInput()).hasValue(expectedValue);
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

    // ── Working days ──────────────────────────────────────────────────────────

    public void checkWorkday(DayOfWeek day) {
        executeUserAction(() -> workdayCheckbox(day).check());
    }

    public void uncheckWorkday(DayOfWeek day) {
        executeUserAction(() -> workdayCheckbox(day).uncheck());
    }

    public void setWorkingHours(double hours) {
        executeUserAction(() -> workingHoursInput().fill(String.valueOf(hours)));
    }

    public void assertWorkdayChecked(DayOfWeek day) {
        assertThat(workdayCheckbox(day)).isChecked();
    }

    public void assertWorkdayUnchecked(DayOfWeek day) {
        assertThat(workdayCheckbox(day)).not().isChecked();
    }

    public void assertWorkingHours(double hours) {
        assertThat(workingHoursInput()).hasValue(String.valueOf(hours));
    }

    private Locator workdayCheckbox(DayOfWeek day) {
        return page.getByTestId("settings-workday-" + day.name().toLowerCase());
    }

    private Locator workingHoursInput() {
        return page.getByTestId("settings-working-time-input");
    }

    public void submit() {
        final Locator submitButton = page.getByTestId("settings-submit-button");
        page.waitForResponse(Response::ok, submitButton::click);
        page.waitForLoadState(DOMCONTENTLOADED);
    }

    private void executeUserAction(Runnable runnable) {
        // updating interactive elements results in a "page-refresh" when javascript is enabled
        page.waitForResponse(response -> response.ok() || response.status() == 422, runnable);
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
