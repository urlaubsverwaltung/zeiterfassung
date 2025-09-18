package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.ui.extension.UiTest;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import de.focusshift.zeiterfassung.ui.pages.NavigationPage;
import de.focusshift.zeiterfassung.ui.pages.ReportPage;
import de.focusshift.zeiterfassung.ui.pages.SettingsPage;
import de.focusshift.zeiterfassung.ui.pages.TimeEntryDialogPage;
import de.focusshift.zeiterfassung.ui.pages.TimeEntryPage;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.microsoft.playwright.options.WaitForSelectorState.VISIBLE;
import static de.focusshift.zeiterfassung.ui.pages.LoginPage.Credentials.credentials;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers(parallel = true)
@UiTest
class TimeEntryUIIT {

    private static final ZoneId USER_ZONE_ID = ZoneId.of("Europe/Berlin");

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

    @MockitoBean
    private UserSettingsProvider userSettingsProvider;

    @BeforeEach
    void setUp() {
        when(userSettingsProvider.zoneId()).thenReturn(USER_ZONE_ID);
        when(userSettingsProvider.firstDayOfWeek()).thenReturn(DayOfWeek.MONDAY);
    }

    @Test
    void ensureTimeEntryCreationNotAllowedForLockedDate(Page page) {

        final NavigationPage navigationPage = new NavigationPage(page);
        final SettingsPage settingsPage = new SettingsPage(page);
        final TimeEntryPage timeEntryPage = new TimeEntryPage(page);
        final LoginPage loginPage = new LoginPage(page, port);

        loginPage.login(credentials("office", "secret"));

        navigationPage.goToSettingsPage();

        settingsPage.assertLockTimeEntriesNotChecked();

        settingsPage.enableLockTimeEntries();
        settingsPage.setLockTimeEntriesDaysInPast("0");
        settingsPage.submit();

        navigationPage.logout();

        loginPage.login(credentials("user", "secret"));

        final LocalDate yesterday = LocalDate.now(USER_ZONE_ID).minusDays(1);
        timeEntryPage.fillNewTimeEntry(yesterday, LocalTime.parse("08:00"), LocalTime.parse("17:00"), "");
        timeEntryPage.submitNewTimeEntryButton().click();

        final Locator error = page.getByText("Für den Zeitraum kann keine Zeit mehr erfasst werden. Bitte wende dich an eine berechtigte Person.");
        error.waitFor(new Locator.WaitForOptions().setState(VISIBLE));
        assertThat(error).isVisible();
    }

    @Test
    void ensureTimeEntryHistoryIsDisplayed(Page page) {

        final NavigationPage navigationPage = new NavigationPage(page);
        final TimeEntryPage timeEntryPage = new TimeEntryPage(page);
        final ReportPage reportPage = new ReportPage(page);
        final TimeEntryDialogPage timeEntryDialogPage = new TimeEntryDialogPage(page);
        final LoginPage loginPage = new LoginPage(page, port);

        loginPage.login(credentials("office", "secret"));

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime start = now.withHour(9).withMinute(0).withSecond(0).withNano(0);
        final LocalDateTime end = now.withHour(9).withMinute(30).withSecond(0).withNano(0);

        timeEntryPage.fillNewTimeEntry(start.toLocalDate(), start.toLocalTime(), end.toLocalTime(), "comment");
        timeEntryPage.submitNewTimeEntryButton().click();

        final Locator commentInputLocator = timeEntryPage.getCommentInput("comment");
        commentInputLocator.fill("comment updated");
        timeEntryPage.submitTimeEntryHaving(commentInputLocator);

        navigationPage.goToReportsPage();

        reportPage.timeEntryDialogButtonLocator("comment updated").click();

        timeEntryDialogPage.showsTimeEntryHistoryElement(start.toLocalDate(), start.toLocalTime(), end.toLocalTime(), "comment updated");
        timeEntryDialogPage.showsTimeEntryHistoryElement(start.toLocalDate(), start.toLocalTime(), end.toLocalTime(), "comment");
    }
}
