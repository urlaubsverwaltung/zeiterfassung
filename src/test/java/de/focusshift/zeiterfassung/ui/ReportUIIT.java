package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.ui.extension.UiTest;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import de.focusshift.zeiterfassung.ui.pages.NavigationPage;
import de.focusshift.zeiterfassung.ui.pages.ReportPage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@UiTest
class ReportUIIT {

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
    void ensureReportsForAllPersons(Page page) {

        final NavigationPage navigationPage = new NavigationPage(page);
        final ReportPage reportPage = new ReportPage(page);

        // persons are not visible until first login
        login("user", "secret", page);
        navigationPage.logout();

        login("office", "secret", page);

        navigationPage.goToReportsPage();

        assertThat(reportPage.personDetailTableLocator()).not().isAttached();

        // select everyone
        final ReportPage.PersonSelect personSelect = reportPage.getPersonSelect();
        final Locator everyoneLocator = personSelect.everyoneLocator();
        assertThat(personSelect.locator()).hasAttribute("aria-expanded", "false");
        personSelect.locator().click();
        assertThat(personSelect.locator()).hasAttribute("aria-expanded", "true");
        assertThat(everyoneLocator).not().isChecked();
        everyoneLocator.setChecked(true);
        personSelect.submit();

        assertThat(reportPage.personDetailTableLocator()).isVisible();
        assertThat(reportPage.personDetailTableLocator()).containsText("Klaus Müller");
        assertThat(reportPage.personDetailTableLocator()).containsText("Marlene Muster");
    }

    private void login(String username, String password, Page page) {
        page.navigate("http://localhost:" + port + "/oauth2/authorization/keycloak");
        new LoginPage(page).login(new LoginPage.Credentials(username, password));
    }
}
