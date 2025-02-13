package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

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

        container.locator("[data-test-id=input-time-entry-start]").fill(TIME_FORMATTER.format(start));
        container.locator("[data-test-id=input-time-entry-end]").fill(TIME_FORMATTER.format(end));
        container.locator("[data-test-id=input-time-entry-comment]").fill(comment);
    }

    public Locator submitNewTimeEntryButton() {
        return timeEntryCreateContainer().locator("[data-test-id=submit-time-entry]");
    }

    /**
     * Returns {@link Locator} of the comment input currently having the given value.
     *
     * @param value value of the input element
     * @return {@link Locator} of the comment input currently having the given value.
     */
    public Locator getCommentInput(String value) {
        return page.locator("css=[data-test-id=input-time-entry-comment][value='%s']".formatted(value));
    }

    public void submitTimeEntryHaving(Locator childLocator) {
        timeEntryEditContainer(childLocator).locator("[data-test-id=submit-time-entry]").click();
    }

    private Locator timeEntryEditContainer(Locator childLocator) {
        return page.locator("[data-test-id=time-entry-form]").filter(new Locator.FilterOptions().setHas(childLocator));
    }

    private Locator timeEntryCreateContainer() {
        return page.locator("[data-test-id=time-entry-create-container]");
    }
}
