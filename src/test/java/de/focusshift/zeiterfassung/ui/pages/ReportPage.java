package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class ReportPage {

    private final Page page;

    public ReportPage(Page page) {
        this.page = page;
    }

    public Locator timeEntryDialogButtonLocator(String comment) {
        return page.locator("[data-test-id=report-time-entry]")
            .filter(new Locator.FilterOptions().setHasText(comment))
            .locator("[data-test-id=report-time-entry-detail-button]");
    }
}
