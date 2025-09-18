package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.ui.extension.UiTest;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import de.focusshift.zeiterfassung.ui.pages.NavigationPage;
import de.focusshift.zeiterfassung.ui.pages.UsersPage;
import de.focusshift.zeiterfassung.ui.pages.users.UserPermissionsPage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.focusshift.zeiterfassung.ui.pages.LoginPage.Credentials.credentials;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers(parallel = true)
@UiTest
class PermissionUIIT {

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
    void ensurePermissionsAreImmediatelyActiveWithoutNewLogin(Page boss_page) {

        final NavigationPage boss_navigationPage = new NavigationPage(boss_page);
        final UsersPage boss_usersPage = new UsersPage(boss_page);
        final UserPermissionsPage boss_userPermissionsPage = new UserPermissionsPage(boss_page);

        final String user_username = "Klaus Müller";
        final Page user_page = boss_page.context().browser().newContext().newPage();
        final NavigationPage user_navigationPage = new NavigationPage(user_page);
        final LoginPage loginPageBoss = new LoginPage(boss_page, port);
        final LoginPage loginPageUser = new LoginPage(user_page, port);

        loginPageBoss.login(credentials("boss", "secret"));

        loginPageUser.login(credentials("user", "secret"));
        assertThat(user_navigationPage.usersLink()).not().isAttached();

        // Boss: add permission to user
        boss_navigationPage.goToUsersPage();
        boss_usersPage.selectPerson(user_username);
        boss_usersPage.goToPermissionsSettings();
        assertThat(boss_userPermissionsPage.getAllowedToEditPermissionsCheckbox()).not().isChecked();
        boss_userPermissionsPage.getAllowedToEditPermissionsCheckbox().click();
        boss_userPermissionsPage.submit();

        // User: navigation should be updated
        user_page.waitForResponse(Response::ok, user_page::reload);
        assertThat(user_navigationPage.usersLink()).isAttached();

        // Boss: remove permission from user
        assertThat(boss_userPermissionsPage.getAllowedToEditPermissionsCheckbox()).isChecked();
        boss_userPermissionsPage.getAllowedToEditPermissionsCheckbox().click();
        boss_userPermissionsPage.submit();

        // User: navigation should be updated
        user_page.waitForResponse(Response::ok, user_page::reload);
        assertThat(user_navigationPage.usersLink()).not().isAttached();

        user_navigationPage.logout();
        boss_navigationPage.logout();
    }
}
