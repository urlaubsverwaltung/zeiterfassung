package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

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

        // the input auto-submits itself, debounced by data-auto-submit-delay (see user-search.html).
        // wait for that submission, otherwise an old suggestion could be clicked while it is re-rendered.
        page.waitForResponse(response -> response.ok() && response.url().contains("query=" + query),
            () -> userSearchLocator().fill(query));

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
