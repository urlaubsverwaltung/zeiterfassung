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
        return page.locator("[data-test-id=works-on-public-holiday-checkbox]");
    }

    public Locator federalStateSelect() {
        return page.locator("[data-test-id=federal-state-select]");
    }

    public void submit() {
        final Locator submitButton = page.locator("[data-test-id=settings-submit-button]");
        page.waitForResponse(Response::ok, submitButton::click);
        page.waitForLoadState(DOMCONTENTLOADED);
    }
}
