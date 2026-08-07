package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.ui.extension.UiIntegrationTest;
import de.focusshift.zeiterfassung.ui.extension.UiTest;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import de.focusshift.zeiterfassung.ui.pages.NavigationPage;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.focusshift.zeiterfassung.ui.pages.LoginPage.Credentials.USER;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers(parallel = true)
@UiIntegrationTest
@UiTest
class NavigationUIIT {

    // narrower than the `desktop` breakpoint (1280px) so the mobile hamburger menu / drawer is used
    private static final int MOBILE_VIEWPORT_WIDTH = 390;
    private static final int MOBILE_VIEWPORT_HEIGHT = 844;

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
    void ensureMobileNavigationDrawerOpensFullyAndShowsLinks(Page page) {

        page.setViewportSize(MOBILE_VIEWPORT_WIDTH, MOBILE_VIEWPORT_HEIGHT);

        final LoginPage loginPage = new LoginPage(page, port);
        final NavigationPage navigationPage = new NavigationPage(page);

        loginPage.login(USER);

        navigationPage.openMobileMenu();

        assertThat(navigationPage.mobileMenu()).isVisible();
        assertThat(navigationPage.timeEntryLink()).isVisible();

        // regression guard: the drawer previously collapsed to a small anchor-sized box (a few dozen px)
        // instead of covering the viewport, on browsers where the anchor-positioning-based sizing broke.
        final BoundingBox box = navigationPage.mobileMenu().boundingBox();
        Assertions.assertThat(box).isNotNull();
        Assertions.assertThat(box.width).isGreaterThan(MOBILE_VIEWPORT_WIDTH * 0.9);
        Assertions.assertThat(box.height).isGreaterThan(MOBILE_VIEWPORT_HEIGHT * 0.5);
    }
}
