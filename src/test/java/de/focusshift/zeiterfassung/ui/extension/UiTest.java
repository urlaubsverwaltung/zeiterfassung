package de.focusshift.zeiterfassung.ui.extension;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.ZoneId;

/**
 * Marks a test as belonging to the general "ui" suite of browser-driven tests, so it can be selected or
 * excluded via JUnit/Maven's {@code ui} tag (e.g. {@code -Dgroups=ui}).
 *
 * <p>This annotation is a plain tag — it does not configure any test infrastructure by itself. Combine it
 * with {@link UiIntegrationTest} to also get a Spring context and a Playwright browser.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @SpringBootTest(webEnvironment = RANDOM_PORT)
 * @UiIntegrationTest
 * @UiTest
 * class TestExampleUIIT {
 *   @Test
 *   void shouldProvidePage(Page page) {
 *     page.navigate("https://playwright.dev");
 *     assertThat(page).hasURL("https://playwright.dev/");
 *   }
 * }
 * }</pre>
 *
 * @see A11yTest
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("ui")
public @interface UiTest {

    ZoneId USER_ZONE_ID = ZoneId.of("Europe/Berlin");
}
