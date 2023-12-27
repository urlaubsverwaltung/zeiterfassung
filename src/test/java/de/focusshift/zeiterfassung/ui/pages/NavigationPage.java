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
        page.waitForResponse(Response::ok, () -> usersLink().click());
        page.waitForLoadState(DOMCONTENTLOADED);
    }

    public Locator usersLink() {
        return page.locator("[data-test-id=navigation-link-users]");
    }

    private record AvatarMenu(Page page) {

        void logout() {
            page.context().clearCookies();
//            page.locator("[data-test-id=avatar]").click();
            // TODO clicking logout renders a white page without Response.ok()
//            page.waitForResponse(Response::ok, () -> page.locator("[data-test-id=logout]").click());
//            page.waitForLoadState(DOMCONTENTLOADED);
        }
    }
}
