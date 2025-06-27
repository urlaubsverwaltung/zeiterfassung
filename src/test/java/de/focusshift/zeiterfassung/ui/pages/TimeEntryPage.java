package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeEntryPage {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm");

    private final Page page;

    public TimeEntryPage(Page page) {
        this.page = page;
    }

    public void fillNewTimeEntry(LocalDate date, LocalTime start, LocalTime end, String comment) {
        final Locator container = timeEntryCreateContainer();

        // not really what the user does... but I didn't want to implement the clicks...
        container.locator("duet-date-picker")
            .evaluate("(node, date) => { node.value = date }", DATE_FORMATTER.format(date));

        container.getByTestId("input-time-entry-start").fill(TIME_FORMATTER.format(start));
        container.getByTestId("input-time-entry-end").fill(TIME_FORMATTER.format(end));
        container.getByTestId("input-time-entry-comment").fill(comment);
    }

    public Locator submitNewTimeEntryButton() {
        return timeEntryCreateContainer().getByTestId("submit-time-entry");
    }

    /**
     * Returns {@link Locator} of the comment input currently having the given value.
     *
     * @param value value of the input element
     * @return {@link Locator} of the comment input currently having the given value.
     */
    public Locator getCommentInput(String value) {
        return page.locator("css=[data-testid=input-time-entry-comment][value='%s']".formatted(value));
    }

    public void submitTimeEntryHaving(Locator childLocator) {
        page.waitForResponse(Response::ok, () -> {
            timeEntryEditContainer(childLocator).getByTestId("submit-time-entry").click();
        });
    }

    private Locator timeEntryEditContainer(Locator childLocator) {
        return page.getByTestId("time-entry-form").filter(new Locator.FilterOptions().setHas(childLocator));
    }

    private Locator timeEntryCreateContainer() {
        return page.getByTestId("time-entry-create-container");
    }
}
