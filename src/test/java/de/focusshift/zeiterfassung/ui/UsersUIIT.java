package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Page;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.ui.extension.UiTest;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import de.focusshift.zeiterfassung.ui.pages.NavigationPage;
import de.focusshift.zeiterfassung.ui.pages.UsersPage;
import de.focusshift.zeiterfassung.ui.pages.users.UserOvertimeAccountPage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers(parallel = true)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@UiTest
class UsersUIIT {

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
    void ensureOvertimeAccount(Page page) {

        final LoginPage loginPage = new LoginPage(page, port);
        final NavigationPage navigationPage = new NavigationPage(page);
        final UsersPage usersPage = new UsersPage(page);
        final UserOvertimeAccountPage overtimeAccountPage = new UserOvertimeAccountPage(page);

        page.navigate("http://localhost:" + port + "/oauth2/authorization/keycloak");

        loginPage.login(new LoginPage.Credentials("boss", "secret"));
        navigationPage.goToUsersPage();

        usersPage.selectPerson("Max Mustermann");
        usersPage.goToOvertimeAccountSettings();

        assertThat(overtimeAccountPage.overtimeAllowedInput()).isChecked();
        overtimeAccountPage.clickOvertimeAllowed();
        overtimeAccountPage.submit();

        assertThat(overtimeAccountPage.overtimeAllowedInput()).not().isChecked();

        // revert to default
        overtimeAccountPage.clickOvertimeAllowed();
        overtimeAccountPage.submit();
        assertThat(overtimeAccountPage.overtimeAllowedInput()).isChecked();

        navigationPage.logout();
    }
}
