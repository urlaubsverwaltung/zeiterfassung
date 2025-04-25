package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import static com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED;

public class SettingsPage {

    private final Page page;

    public SettingsPage(Page page) {
        this.page = page;
    }

    public Locator worksOnPublicHolidayCheckbox() {
        return page.getByTestId("works-on-public-holiday-checkbox");
    }

    public Locator federalStateSelect() {
        return page.getByTestId("federal-state-select");
    }

    public Locator lockTimeEntriesCheckbox() {
        return page.getByTestId("locking-timeentries-is-active-checkbox");
    }

    public Locator lockTimEntriesDaysInPastInput() {
        return page.getByTestId("lock-timeentries-days-in-past");
    }

    public void submit() {
        final Locator submitButton = page.getByTestId("settings-submit-button");
        page.waitForResponse(Response::ok, submitButton::click);
        page.waitForLoadState(DOMCONTENTLOADED);
    }
}
