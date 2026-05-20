package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Page;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.ui.extension.UiTest;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import de.focusshift.zeiterfassung.ui.pages.NavigationPage;
import de.focusshift.zeiterfassung.ui.pages.ReportPage;
import de.focusshift.zeiterfassung.ui.pages.TimeEntryPage;
import de.focusshift.zeiterfassung.ui.pages.UserSearchPage;
import de.focusshift.zeiterfassung.ui.pages.UsersPage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static de.focusshift.zeiterfassung.ui.pages.LoginPage.Credentials.BOSS;
import static de.focusshift.zeiterfassung.ui.pages.LoginPage.Credentials.OFFICE;
import static de.focusshift.zeiterfassung.ui.pages.LoginPage.Credentials.USER;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers(parallel = true)
@UiTest
class UserSearchUIIT {

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

    @Test
    void ensureUserSearch(Page page) {
        final LoginPage loginPage = new LoginPage(page, port);
        final NavigationPage navigationPage = new NavigationPage(page);

        // log in with different users first, otherwise they don't exist in the application
        loginPage.login(BOSS);
        navigationPage.logout();

        loginPage.login(USER);
        navigationPage.logout();

        // then ensure user search for OFFICE
        loginPage.login(OFFICE);

        final UserSearchPage userSearchPage = new UserSearchPage(page);

        ensureTimeEntriesUserSearch(page, navigationPage, userSearchPage);
        ensureReportsUserSearch(page, navigationPage, userSearchPage);
        ensureUserSettingsUserSearch(page, navigationPage, userSearchPage);
        ensureSettingsUserSearch(page, navigationPage, userSearchPage);
        navigationPage.logout();

        // USER is not allowed to search for other users
        loginPage.login(USER);
        userSearchPage.isNotPresent();
    }

    private void ensureTimeEntriesUserSearch(Page page, NavigationPage navigationPage, UserSearchPage userSearchPage) {

        final TimeEntryPage timeEntryPage = new TimeEntryPage(page);

        navigationPage.goToTimeEntryPage();

        userSearchPage.search("max");
        userSearchPage.selectSuggestion("Max Mustermann");

        timeEntryPage.isVisibleForOtherPerson("Max Mustermann");
    }

    private void ensureReportsUserSearch(Page page, NavigationPage navigationPage, UserSearchPage userSearchPage) {

        final ReportPage reportPage = new ReportPage(page);

        navigationPage.goToReportsPage();

        userSearchPage.search("max");
        userSearchPage.selectSuggestion("Max Mustermann");

        reportPage.isVisibleForOtherPerson("Max Mustermann");
    }

    private void ensureUserSettingsUserSearch(Page page, NavigationPage navigationPage, UserSearchPage userSearchPage) {

        final UsersPage usersPage = new UsersPage(page);

        navigationPage.goToUsersPage();

        userSearchPage.search("max");
        userSearchPage.selectSuggestion("Max Mustermann");

        usersPage.isVisibleForOtherPerson("Max Mustermann");
    }

    private void ensureSettingsUserSearch(Page page, NavigationPage navigationPage, UserSearchPage userSearchPage) {

        final TimeEntryPage timeEntryPage = new TimeEntryPage(page);

        navigationPage.goToSettingsPage();

        userSearchPage.search("max");
        userSearchPage.selectSuggestion("Max Mustermann");

        // on settings page the main suggestion link goes to timeentries page
        timeEntryPage.isVisibleForOtherPerson("Max Mustermann");
    }
}
