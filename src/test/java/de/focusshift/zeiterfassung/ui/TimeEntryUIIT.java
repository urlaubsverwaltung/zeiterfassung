package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.ui.extension.UiTest;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import de.focusshift.zeiterfassung.ui.pages.NavigationPage;
import de.focusshift.zeiterfassung.ui.pages.ReportPage;
import de.focusshift.zeiterfassung.ui.pages.TimeEntryDialogPage;
import de.focusshift.zeiterfassung.ui.pages.TimeEntryPage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@UiTest
class TimeEntryUIIT {

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
    void ensureTimeEntryHistoryIsDisplayed(Page page) {

        final NavigationPage navigationPage = new NavigationPage(page);
        final TimeEntryPage timeEntryPage = new TimeEntryPage(page);
        final ReportPage reportPage = new ReportPage(page);
        final TimeEntryDialogPage timeEntryDialogPage = new TimeEntryDialogPage(page);

        login("office", "secret", page);

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

    private void login(String username, String password, Page page) {
        page.navigate("http://localhost:" + port + "/oauth2/authorization/keycloak");
        new LoginPage(page).login(new LoginPage.Credentials(username, password));
    }
}
