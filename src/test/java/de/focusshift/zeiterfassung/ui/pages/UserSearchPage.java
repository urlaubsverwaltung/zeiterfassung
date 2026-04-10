package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.microsoft.playwright.options.AriaRole.LINK;
import static com.microsoft.playwright.options.LoadState.NETWORKIDLE;

public class UserSearchPage {

    private final Page page;

    public UserSearchPage(Page page) {
        this.page = page;
    }

    public void isNotPresent() {
        assertThat(userSearchLocator()).not().isVisible();
    }

    public void search(String query) {

        // search input focus opens the suggestions popover
        userSearchLocator().focus();
        assertThat(userSuggestionsLocator()).isVisible();

        userSearchLocator().fill(query);

        // wait for rerendered suggestions otherwise an old link outside the DOM could be tried to be clicked.
        // networkidle should be ok, we don't have long-running stuff
        page.waitForLoadState(NETWORKIDLE);
    }

    public void selectSuggestion(String name) {
        userSuggestionsLocator().getByRole(LINK, new Locator.GetByRoleOptions().setName(name)).click();
    }

    public void selectSuggestionTimeEntries(String name) {
        //
    }

    public void selectSuggestionReports(String name) {
        //
    }

    public void selectSuggestionSettings(String name) {
        //
    }

    private Locator userSearchLocator() {
        return page.locator("input[name=query]");
    }

    private Locator userSuggestionsLocator() {
        return page.getByTestId("user-suggestions");
    }
}
