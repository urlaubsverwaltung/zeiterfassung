package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import static com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED;

public class ReportPage {

    private final Page page;

    public ReportPage(Page page) {
        this.page = page;
    }

    public PersonSelect getPersonSelect() {
        return new PersonSelect();
    }

    public Locator personDetailTableLocator() {
        return page.getByTestId("report-person-detail-table");
    }

    public Locator timeEntryDialogButtonLocator(String comment) {
        return page.getByTestId("report-time-entry")
            .filter(new Locator.FilterOptions().setHasText(comment))
            .getByTestId("report-time-entry-detail-button");
    }

    public class PersonSelect {

        public Locator locator() {
            return page.getByTestId("report-person-select");
        }

        public Locator everyoneLocator() {
            return page.getByTestId("report-person-select-everyone");
        }

        public void submit() {
            final Locator submitButton = page.getByTestId("report-person-select-submit");
            page.waitForResponse(Response::ok, submitButton::click);
            page.waitForLoadState(DOMCONTENTLOADED);
        }
    }
}
