package de.focusshift.zeiterfassung.ui.pages.users;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import java.time.DayOfWeek;
import java.util.List;

import static com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED;

/**
 * Page to create or edit a {@linkplain de.focusshift.zeiterfassung.workingtime.WorkingTime} of a person.
 */
public class UserWorkingTimePage {

    private final Page page;

    public UserWorkingTimePage(Page page) {
        this.page = page;
    }

    public Locator federalStateSelect() {
        return page.getByTestId("federal-state-select");
    }

    public Locator worksOnPublicHolidayGlobalButton() {
        return page.getByTestId("works-on-public-holiday-global-input");
    }

    public Locator worksOnPublicHolidayYesButton() {
        return page.getByTestId("works-on-public-holiday-yes-input");
    }

    public Locator worksOnPublicHolidayNoButton() {
        return page.getByTestId("works-on-public-holiday-no-input");
    }

    public void selectWorkdays(List<DayOfWeek> weekdays) {
        for (DayOfWeek weekday : weekdays) {
            page.locator("[name=workday][value=%s]".formatted(weekday.name().toLowerCase())).click();
        }
    }

    public Locator workingTimeHoursInput() {
        return page.getByTestId("working-time-hours-input");
    }

    public void submit() {
        final Locator submitButton = page.getByTestId("working-time-submit-button");
        page.waitForResponse(Response::ok, submitButton::click);
        page.waitForLoadState(DOMCONTENTLOADED);
    }
}
