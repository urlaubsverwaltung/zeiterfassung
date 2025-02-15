package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class TimeEntryDialogPage {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm");

    private final Page page;

    public TimeEntryDialogPage(Page page) {
        this.page = page;
    }

    public void showsTimeEntryHistoryElement(LocalDate date, LocalTime start, LocalTime end, String comment) {

        // no test-id because the datepicker js must handle the input attribute somehow
        // adding it to the duet-date-picker invisible input? or adding it to the duet-date-picker? we will see...
        final Locator dateInputLocator = page.locator("input[type=date]");

        final Locator startInputLocator = page.getByTestId("input-time-entry-start");
        final Locator endInputLocator = page.getByTestId("input-time-entry-end");
        final Locator commentInputLocator = page.getByTestId("input-time-entry-comment");

        final Locator historyItem = page.getByTestId("time-entry-history-item")
            .filter(havingChild(dateInputLocator.and(page.locator("[value='%s']".formatted(DATE_FORMATTER.format(date))))))
            .filter(havingChild(startInputLocator.and(page.locator("[value='%s']".formatted(TIME_FORMATTER.format(start))))))
            .filter(havingChild(endInputLocator.and(page.locator("[value='%s']".formatted(TIME_FORMATTER.format(end))))))
            .filter(havingChild(commentInputLocator.and(page.locator("[value='%s']".formatted(comment)))));

        assertThat(historyItem).isVisible();
    }

    private static Locator.FilterOptions havingChild(Locator locator) {
        return new Locator.FilterOptions().setHas(locator);
    }
}
