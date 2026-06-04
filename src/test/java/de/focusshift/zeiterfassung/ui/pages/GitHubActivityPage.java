package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.time.LocalDate;
import java.util.List;

import static com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED;

public class GitHubActivityPage {

    private final Page page;
    private final int port;

    public GitHubActivityPage(Page page, int port) {
        this.page = page;
        this.port = port;
    }

    public void navigate(LocalDate date) {
        page.navigate("http://localhost:" + port + "/github-activity?date=" + date);
        page.waitForLoadState(DOMCONTENTLOADED);
    }

    public void navigate() {
        navigate(LocalDate.now());
    }

    // ── Section locators ──────────────────────────────────────────────────────

    public Locator noGithubLoginSection() {
        return page.getByTestId("github-activity-no-login");
    }

    public Locator prSection() {
        return page.getByTestId("github-activity-pr-section");
    }

    public Locator reviewsSection() {
        return page.getByTestId("github-activity-reviews-section");
    }

    public Locator issuesSection() {
        return page.getByTestId("github-activity-issues-section");
    }

    public Locator standaloneSection() {
        return page.getByTestId("github-activity-standalone-section");
    }

    public Locator syncNotConfiguredBanner() {
        return page.getByTestId("github-activity-sync-not-configured");
    }

    public Locator emptyState() {
        return page.getByTestId("github-activity-empty");
    }

    // ── Table column headers ──────────────────────────────────────────────────

    public List<String> prTableColumnHeaders() {
        return prSection().locator("thead th")
            .allTextContents()
            .stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    }

    public List<String> reviewsTableColumnHeaders() {
        return reviewsSection().locator("thead th")
            .allTextContents()
            .stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    }

    public List<String> issuesTableColumnHeaders() {
        return issuesSection().locator("thead th")
            .allTextContents()
            .stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    }

    // ── Table row locators ────────────────────────────────────────────────────

    public Locator prTableRows() {
        return prSection().locator("tbody tr").filter(new Locator.FilterOptions()
            .setHasNot(page.locator("turbo-frame")));
    }

    public Locator reviewTableRows() {
        return reviewsSection().locator("tbody tr").filter(new Locator.FilterOptions()
            .setHasNot(page.locator("turbo-frame")));
    }

    public Locator issueTableRows() {
        return issuesSection().locator("tbody tr").filter(new Locator.FilterOptions()
            .setHasNot(page.locator("turbo-frame")));
    }

    // ── PR row details ────────────────────────────────────────────────────────

    public Locator prBadgeLink(int rowIndex) {
        return prTableRows().nth(rowIndex).locator("a[href*='/pull/']").first();
    }

    public Locator prTitleLink(int rowIndex) {
        return prTableRows().nth(rowIndex).locator("td:nth-child(3) a");
    }

    public String prStatus(int rowIndex) {
        return prTableRows().nth(rowIndex).locator("td:nth-child(5) span").textContent().trim();
    }

    // ── Log-it / Logged locators (PR) ────────────────────────────────────────

    public Locator prLogItButton(int rowIndex) {
        return page.getByTestId("pr-log-it-" + rowIndex);
    }

    public Locator prLoggedLabel(int rowIndex) {
        return page.getByTestId("pr-logged-" + rowIndex);
    }

    // ── Log-it / Logged locators (Reviews) ───────────────────────────────────

    public Locator reviewLogItButton(int rowIndex) {
        return page.getByTestId("rev-log-it-" + rowIndex);
    }

    public Locator reviewLoggedLabel(int rowIndex) {
        return page.getByTestId("rev-logged-" + rowIndex);
    }

    // ── Log-it / Logged locators (Issues) ────────────────────────────────────

    public Locator issueLogItButton(int rowIndex) {
        return page.getByTestId("iss-log-it-" + rowIndex);
    }

    public Locator issueLoggedLabel(int rowIndex) {
        return page.getByTestId("iss-logged-" + rowIndex);
    }

    // ── Log-it / Logged locators (Standalone commits) ─────────────────────────

    public Locator standaloneLogItButton(int anchorIndex, int eventIndex) {
        return page.getByTestId("ev-log-it-" + anchorIndex + "-" + eventIndex);
    }

    public Locator standaloneLoggedLabel(int anchorIndex, int eventIndex) {
        return page.getByTestId("ev-logged-" + anchorIndex + "-" + eventIndex);
    }

    // ── Inline form ───────────────────────────────────────────────────────────

    /** The Save button inside the inline form (loaded into a turbo-frame after clicking Log it). */
    public Locator inlineFormSaveButton() {
        return page.locator("turbo-frame form button[type=submit]");
    }

    // ── Standalone commit row locators ────────────────────────────────────────

    public Locator standaloneCommitLinks() {
        return standaloneSection().locator("a[href*='/commit/']");
    }

    // ── Search modal ──────────────────────────────────────────────────────────

    public Locator searchTrigger() {
        return page.getByTestId("gh-search-trigger");
    }

    public Locator searchModal() {
        return page.getByTestId("gh-search-modal");
    }

    public Locator searchInput() {
        return page.getByTestId("gh-search-input");
    }

    public Locator searchTabDay() {
        return page.getByTestId("gh-search-tab-day");
    }

    public Locator searchTabAll() {
        return page.getByTestId("gh-search-tab-all");
    }

    public Locator searchResults() {
        return page.getByTestId("gh-search-results");
    }

    public Locator searchResultItems() {
        return page.getByTestId("gh-search-result");
    }

    public Locator searchEmptyState() {
        return page.getByTestId("gh-search-empty");
    }

    public Locator searchAllDatesLink() {
        return page.getByTestId("gh-search-all-link");
    }

    public Locator searchLogItButtons() {
        return page.getByTestId("gh-search-log-it");
    }

    public void openSearchModal() {
        searchTrigger().click();
    }

    public void typeInSearch(String text) {
        searchInput().fill(text);
    }
}
