package de.focusshift.zeiterfassung.ui.pages.users;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Page that shows all {@linkplain de.focusshift.zeiterfassung.workingtime.WorkingTime}s of a person.
 */
public class UserWorkingTimeAccountPage {

    private final Page page;

    public UserWorkingTimeAccountPage(Page page) {
        this.page = page;
    }

    public Locator createNewWorkingTimeButton() {
        return page.getByTestId("working-time-create-button");
    }
}
