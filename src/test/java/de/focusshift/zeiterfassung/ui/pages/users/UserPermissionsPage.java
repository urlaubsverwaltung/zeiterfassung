package de.focusshift.zeiterfassung.ui.pages.users;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import static com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED;

public class UserPermissionsPage {

    private final Page page;

    public UserPermissionsPage(Page page) {
        this.page = page;
    }

    public Locator getAllowedToEditPermissionsCheckbox() {
        return page.getByTestId("permissions-edit-all-checkbox");
    }

    public void submit() {
        final Locator submitButton = page.getByTestId("permissions-submit-button");
        page.waitForResponse(Response::ok, submitButton::click);
        page.waitForLoadState(DOMCONTENTLOADED);
    }
}
