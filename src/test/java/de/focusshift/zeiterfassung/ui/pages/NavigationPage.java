package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import static com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED;

/**
 * The NavigationPage class represents a page in a web application that provides navigation functionality.
 */
public class NavigationPage {

    private final Page page;
    private final AvatarMenu avatarMenu;

    public NavigationPage(Page page) {
        this.avatarMenu = new AvatarMenu(page);
        this.page = page;
    }

    public void logout() {
        avatarMenu.logout();
    }

    public void goToUsersPage() {
        goTo(usersLink());
    }

    public void goToSettingsPage() {
        goTo(settingsLink());
    }

    public void goToReportsPage() {
        goTo(reportsLink());
    }

    public Locator usersLink() {
        return page.locator("[data-test-id=navigation-link-users]");
    }

    public Locator settingsLink() {
        return page.locator("[data-test-id=navigation-link-settings]");
    }

    public Locator reportsLink() {
        return page.locator("[data-test-id=navigation-link-reports]");
    }

    private void goTo(Locator locator) {
        page.waitForResponse(Response::ok, locator::click);
        page.waitForLoadState(DOMCONTENTLOADED);
    }

    private record AvatarMenu(Page page) {

        void logout() {
            page.locator("[data-test-id=avatar]").click();
            page.waitForResponse(Response::ok, () -> page.locator("[data-test-id=logout]").click());
            page.waitForLoadState(DOMCONTENTLOADED);
        }
    }
}
