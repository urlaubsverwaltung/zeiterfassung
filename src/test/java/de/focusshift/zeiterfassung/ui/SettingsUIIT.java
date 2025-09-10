package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Page;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.ui.extension.UiTest;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import de.focusshift.zeiterfassung.ui.pages.NavigationPage;
import de.focusshift.zeiterfassung.ui.pages.SettingsPage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@UiTest
class SettingsUIIT {

    public static final DateTimeFormatter FORMATTER_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
    void ensureSettingsPreviewAndSave(Page page) {

        final NavigationPage navigationPage = new NavigationPage(page);
        final SettingsPage settingsPage = new SettingsPage(page);
        final LoginPage loginPage = new LoginPage(page, port);

        login("office", "secret", loginPage);

        navigationPage.goToSettingsPage();

        settingsPage.assertLockTimeEntriesDaysInPastInputValue("2");

        final String expectedDatePreviewText = LocalDate.now().minusDays(6).format(FORMATTER_DD_MM_YYYY);
        settingsPage.setLockTimeEntriesDaysInPast("5");
        assertThat(page.getByText(Pattern.compile(expectedDatePreviewText))).isVisible();

        // input must not be saved and values should be reset after reload
        page.reload();
        settingsPage.assertLockTimeEntriesDaysInPastInputValue("2");

        settingsPage.setLockTimeEntriesDaysInPast("5");
        settingsPage.submit();

        settingsPage.assertLockTimeEntriesDaysInPastInputValue("5");
        assertThat(page.getByText(Pattern.compile(expectedDatePreviewText))).isVisible();
    }

    private void login(String username, String password, LoginPage loginPage) {
        loginPage.login(new LoginPage.Credentials(username, password));
    }
}
