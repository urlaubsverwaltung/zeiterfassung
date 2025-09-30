package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Page;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.ui.extension.UiTest;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import de.focusshift.zeiterfassung.ui.pages.NavigationPage;
import de.focusshift.zeiterfassung.ui.pages.SettingsPage;
import de.focusshift.zeiterfassung.ui.pages.UsersPage;
import de.focusshift.zeiterfassung.ui.pages.users.UserWorkingTimeAccountPage;
import de.focusshift.zeiterfassung.ui.pages.users.UserWorkingTimePage;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.MessageSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BERLIN;
import static de.focusshift.zeiterfassung.ui.pages.FederalStateSelect.federalStateSelectValue;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.util.Locale.GERMAN;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers(parallel = true)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@UiTest
class UsersWorkingTimeUIIT {

    @LocalServerPort
    private int port;

    @Container
    private static final SingleTenantPostgreSQLContainer postgre = new SingleTenantPostgreSQLContainer();
    @Container
    private static final TestKeycloakContainer keycloak = new TestKeycloakContainer();

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        postgre.configureSpringDataSource(registry);
        keycloak.configureSpringDataSource(registry);
    }

    @Autowired
    private MessageSource messageSource;

    @Test
    void ensureWorkingTime(Page page) {

        final LoginPage loginPage = new LoginPage(page, port);
        final NavigationPage navigationPage = new NavigationPage(page);
        final UsersPage usersPage = new UsersPage(page);
        final UserWorkingTimeAccountPage workingTimeAccountPage = new UserWorkingTimeAccountPage(page);
        final UserWorkingTimePage workingTimePage = new UserWorkingTimePage(page);

        loginPage.login(new LoginPage.Credentials("boss", "secret"));

        ensureDefaultGlobalWorkingTime(page);
        setGlobalFederalState(page, GERMANY_BADEN_WUERTTEMBERG);

        // ensure updated global settings is default for person
        navigationPage.goToUsersPage();
        usersPage.selectPerson("Max Mustermann");
        usersPage.goToWorkingTimeAccountSettings();

        // update working time settings for person
        workingTimeAccountPage.createNewWorkingTimeButton().click();
        assertThat(workingTimePage.federalStateSelect()).hasValue(federalStateSelectValue(FederalState.GLOBAL));
        assertThat(workingTimePage.worksOnPublicHolidayGlobalButton()).isChecked();

        workingTimePage.federalStateSelect().selectOption(federalStateSelectValue(GERMANY_BERLIN));
        workingTimePage.worksOnPublicHolidayYesButton().click();
        workingTimePage.selectWorkdays(List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY));
        workingTimePage.workingTimeHoursInput().fill("4");
        workingTimePage.workingTimeValidFrom().fill("2025-06-14");
        workingTimePage.submit();

        // ensure global setting is shown for person
        Assertions.assertThat(page.content()).contains(i18n("federalState.GERMANY_BERLIN"));

        navigationPage.logout();
    }

    private String i18n(String key) {
        return i18n(key, new Object[]{});
    }

    private String i18n(String key, Object... args) {
        return messageSource.getMessage(key, args, GERMAN);
    }

    private void ensureDefaultGlobalWorkingTime(Page page) {

        final NavigationPage navigationPage = new NavigationPage(page);
        final SettingsPage settingsPage = new SettingsPage(page);

        navigationPage.goToSettingsPage();

        settingsPage.assertFederalStateValue(FederalState.NONE);
        settingsPage.assertWorksOnPublicHolidayNotChecked();
    }

    private void setGlobalFederalState(Page page, FederalState federalState) {

        final SettingsPage settingsPage = new SettingsPage(page);

        settingsPage.selectFederalState(federalState);
        settingsPage.submit();
    }
}
