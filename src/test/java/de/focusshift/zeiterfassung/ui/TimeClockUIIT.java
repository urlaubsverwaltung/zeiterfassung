package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.ui.extension.UiTest;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.focusshift.zeiterfassung.ui.extension.UiTest.USER_ZONE_ID;
import static de.focusshift.zeiterfassung.ui.pages.LoginPage.Credentials.OFFICE;
import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@UiTest
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers(parallel = true)
class TimeClockUIIT {

    @Autowired
    private Clock clock;

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

    @TestConfiguration
    static class ClockTestConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            final LocalDate date = LocalDate.now(USER_ZONE_ID);
            final LocalTime time = LocalTime.of(17, 0);
            return Clock.fixed(ZonedDateTime.of(date, time, USER_ZONE_ID).toInstant(), UTC);
        }
    }

    @BeforeEach
    void setUp() {
        when(userSettingsProvider.zoneId()).thenReturn(USER_ZONE_ID);
        when(userSettingsProvider.firstDayOfWeek()).thenReturn(DayOfWeek.MONDAY);
    }

    @Test
    void ensureTimeClock(Page page) {
        final LoginPage loginPage = new LoginPage(page, port);
        final TimeClockPage timeClockPage = new TimeClockPage(page);

        // fix browser time to match server clock (Date.now() in client code)
        page.clock().setFixedTime(Date.from(Instant.now(clock)));

        loginPage.login(OFFICE);

        timeClockPage.ensureTimeClockNotRunning();

        timeClockPage.startTimeClock();
        timeClockPage.ensureTimeClockRunning();

        final ZonedDateTime now = ZonedDateTime.now(clock.withZone(USER_ZONE_ID));

        timeClockPage.openEditForm();

        final LocalDateTime now = LocalDateTime.now(clock.withZone(USER_ZONE_ID));
        timeClockPage.hasDate(now.toLocalDate());
        timeClockPage.hasTime(now.toLocalTime());

        // future date not allowed
        timeClockPage.setDate(now.toLocalDate().plusDays(1));
        timeClockPage.submit();
        timeClockPage.hasDateError();

        // future time now allowed
        timeClockPage.setDate(now.toLocalDate());
        timeClockPage.setTime(now.toLocalTime().plusHours(1));
        timeClockPage.submit();
        timeClockPage.hasTimeError();

        // valid edit
        timeClockPage.setDate(now.toLocalDate());
        timeClockPage.setTime(now.toLocalTime().minusHours(1));
        timeClockPage.setComment("most awesome comment");
        timeClockPage.submit();
        timeClockPage.ensurePopoverHidden();

        timeClockPage.openEditForm();
        timeClockPage.hasDate(now.toLocalDate());
        timeClockPage.hasTime(now.toLocalTime().minusHours(1));
        timeClockPage.hasComment("most awesome comment");

        timeClockPage.stopTimeClock();
        timeClockPage.ensureTimeClockNotRunning();
    }

    private static class TimeClockPage {
        private final Locator startButton;
        private final Locator stopButton;
        private final Locator editButton;
        private final Locator updateButton;
        private final Locator dateInput;
        private final Locator timeInput;
        private final Locator commentInput;

        private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        private TimeClockPage(Page page) {
            this.startButton = page.getByTestId("time-clock-start-button");
            this.stopButton = page.getByTestId("time-clock-stop-button");
            this.editButton = page.getByTestId("time-clock-edit-button");
            this.updateButton = page.getByTestId("time-clock-update-button");
            this.dateInput = page.getByTestId("time-clock-date-input");
            this.timeInput = page.getByTestId("time-clock-time-input");
            this.commentInput = page.getByTestId("time-clock-comment-input");
        }

        public void startTimeClock() {
            startButton.click();
        }

        public void stopTimeClock() {
            stopButton.click();
        }

        public void openEditForm() {
            editButton.click();
        }

        public void setDate(LocalDate date) {
            dateInput.fill(dateFormatter.format(date));
        }

        public void setTime(LocalTime time) {
            timeInput.fill(timeFormatter.format(time));
        }

        public void setComment(String comment) {
            commentInput.fill(comment);
        }

        public void hasDate(LocalDate date) {
            assertThat(dateInput).hasValue(dateFormatter.format(date));
        }

        public void hasTime(LocalTime time) {
            assertThat(timeInput).hasValue(timeFormatter.format(time));
        }

        public void hasComment(String comment) {
            assertThat(commentInput).hasValue(comment);
        }

        public void submit() {
            updateButton.click();
        }

        public void ensureTimeClockNotRunning() {
            assertThat(startButton).isVisible();
            assertThat(stopButton).not().isVisible();
            assertThat(editButton).not().isVisible();
        }

        public void ensureTimeClockRunning() {
            assertThat(startButton).not().isVisible();
            assertThat(stopButton).isVisible();
            assertThat(editButton).isVisible();
        }

        public void ensurePopoverHidden() {
            assertThat(updateButton).not().isVisible();
            assertThat(dateInput).not().isVisible();
            assertThat(timeInput).not().isVisible();
            assertThat(commentInput).not().isVisible();
        }

        public void hasDateError() {
            assertThat(dateInput).hasAttribute("aria-invalid", "true");
        }

        public void hasTimeError() {
            assertThat(timeInput).hasAttribute("aria-invalid", "true");
        }
    }
}
