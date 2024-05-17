package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import static com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED;

/**
 * The UsersPage class represents a page in a web application that displays information about users and their permissions.
 * It provides methods to select a person, navigate to the permissions page, and access various links on the page.
 */
public class UsersPage {

    private final Page page;

    public UsersPage(Page page) {
        this.page = page;
    }

    /**
     * Select a person and wait till next page has loaded. The next page depends on the users authorizations.
     * Could be workingTime, overtime, permissions, ...
     *
     * @param name name of the person to select
     */
    public void selectPerson(String name) {
        page.waitForResponse(Response::ok, () -> personLink(name).click());
        page.waitForLoadState(DOMCONTENTLOADED);
    }

    /**
     * go to the overtime-account page of the currently selected person.
     */
    public void goToOvertimeAccountSettings() {
        goTo("[data-test-id=users-overtime-account-link]");
    }

    public void goToWorkingTimeAccountSettings() {
        goTo("[data-test-id=users-working-time-account-link]");
    }

    public void goToPermissionsSettings() {
        goTo("[data-test-id=users-permissions-link]");
    }

    private void goTo(String selector) {
        page.waitForResponse(Response::ok, () -> page.locator(selector).click());
        page.waitForLoadState(DOMCONTENTLOADED);
    }

    private Locator personLink(String name) {
        return page.locator("[data-test-id=users-list-person-link]").filter(hasText(name));
    }

    private Locator.FilterOptions hasText(String text) {
        return new Locator.FilterOptions().setHasText(text);
    }
}
