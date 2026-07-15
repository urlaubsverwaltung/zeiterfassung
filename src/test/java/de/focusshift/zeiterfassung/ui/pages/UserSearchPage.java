package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
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

        assertThat(userSuggestionsLocator().first()).isVisible();
    }

    public void selectSuggestion(String name) {
        final Locator link = suggestionRow(name).getByTestId("user-suggestion-main-link");
        assertThat(link).isVisible();
        link.click();
    }

    public void selectSuggestionTimeEntries(String name) {
        final Locator link = suggestionRow(name).getByTestId("user-suggestion-time-entries-link");
        assertThat(link).isVisible();
        link.click();
    }

    public void selectSuggestionReports(String name) {
        final Locator link = suggestionRow(name).getByTestId("user-suggestion-reports-link");
        assertThat(link).isVisible();
        link.click();
    }

    public void selectSuggestionSettings(String name) {
        final Locator link = suggestionRow(name).getByTestId("user-suggestion-settings-link");
        assertThat(link).isVisible();
        link.click();
    }

    private Locator suggestionRow(String name) {
        return userSuggestionsLocator().getByTestId("user-suggestion-row").filter(new Locator.FilterOptions().setHasText(name));
    }

    private Locator userSearchLocator() {
        return page.locator("input[name=query]");
    }

    private Locator userSuggestionsLocator() {
        return page.getByTestId("user-suggestions");
    }
}
