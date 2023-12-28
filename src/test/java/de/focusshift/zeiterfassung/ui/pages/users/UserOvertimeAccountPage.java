package de.focusshift.zeiterfassung.ui.pages.users;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import static com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED;

public class UserOvertimeAccountPage {

    private final Page page;

    public UserOvertimeAccountPage(Page page) {
        this.page = page;
    }

    public Locator overtimeAllowedInput() {
        return page.locator("[data-test-id=overtime-allowed-input]");
    }

    public void clickOvertimeAllowed() {
        overtimeAllowedInput().click();
    }

    public void submit() {
        page.waitForResponse(Response::ok, () -> page.locator("[data-test-id=overtime-account-submit]").click());
        page.waitForLoadState(DOMCONTENTLOADED);
    }
}
