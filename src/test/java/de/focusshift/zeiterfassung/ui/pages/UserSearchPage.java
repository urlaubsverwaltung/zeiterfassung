package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED;
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

        page.waitForLoadState(NETWORKIDLE);
        page.waitForLoadState(DOMCONTENTLOADED);

        assertThat(userSuggestionsLocator().first()).isVisible();
    }

    public void selectSuggestion(String name) {
        final String nameLowercase = "user-suggestion-link-%s".formatted(name.toLowerCase().replaceAll("\\s+", "-"));
        final Locator suggestion = userSuggestionsLocator().getByTestId(nameLowercase);
        assertThat(suggestion).isVisible();
        suggestion.click();
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
