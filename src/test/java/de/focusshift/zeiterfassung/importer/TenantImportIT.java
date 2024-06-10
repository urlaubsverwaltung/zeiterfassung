package de.focusshift.zeiterfassung.importer;

import de.focusshift.zeiterfassung.TestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeclock.TimeClock;
import de.focusshift.zeiterfassung.timeclock.TimeClockService;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccount;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountService;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workingtime.WorkingTime;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
    properties = {
        "zeiterfassung.tenant.mode=multi",
        "zeiterfassung.tenant.import.enabled=true",
        "zeiterfassung.tenant.import.filesystem.path=./src/test/resources/export_file.json",
    }
)
@TestPropertySource("classpath:application-dev-multitenant.yaml")
class TenantImportIT extends TestContainersBase {

    private static final String TENANT_ID = "bac98fef";
    private static final UserId EXTERNAL_USER_ID = new UserId("58400ef7-1cc9-48cb-93a8-f45c7af186ad");

    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private UserManagementService userManagementService;
    @Autowired
    private OvertimeAccountService overtimeAccountService;
    @Autowired
    private WorkingTimeService workingTimeService;
    @Autowired
    private TimeClockService timeClockService;
    @Autowired
    private TimeEntryService timeEntryService;

    @TestConfiguration
    static class TestConfig {

        @Bean
        TestStateInitializer testStateInitializer(TenantService tenantService) {
            return new TestStateInitializer(tenantService);
        }

        static class TestStateInitializer {

            private final TenantService tenantService;

            TestStateInitializer(TenantService tenantService) {
                this.tenantService = tenantService;
            }

            // must run before TenantImporterComponent to initialize required stuff
            @Order(1)
            @EventListener(ApplicationReadyEvent.class)
            public void onApplicationReady() {
                tenantService.create(TENANT_ID);
            }
        }
    }

    @Test
    void ensureTenantImport() {
        await().untilAsserted(() -> {

            final Optional<User> maybeUser = doWithTenant(
                () -> userManagementService.findUserById(EXTERNAL_USER_ID)
            );

            assertThat(maybeUser).isPresent();

            final User user = maybeUser.get();
            final UserIdComposite userIdComposite = user.userIdComposite();

            ensureUser(user);
            ensureOvertimeAccount(userIdComposite);
            ensureWorkingTime(userIdComposite);
            ensureTimeClocks();
            ensureTimeEntries(userIdComposite);
        });
    }

    private void ensureUser(User user) {
        assertThat(user.givenName()).isEqualTo("Marlene");
        assertThat(user.familyName()).isEqualTo("Muster");
        assertThat(user.email()).isEqualTo(new EMailAddress("office@example.org"));
        assertThat(user.authorities()).containsExactlyInAnyOrder(ZEITERFASSUNG_VIEW_REPORT_ALL, ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL, ZEITERFASSUNG_WORKING_TIME_EDIT_ALL, ZEITERFASSUNG_USER);
    }

    private void ensureOvertimeAccount(UserIdComposite userIdComposite) {

        final OvertimeAccount overtimeAccount = doWithTenant(
            () -> overtimeAccountService.getOvertimeAccount(userIdComposite.localId())
        );

        assertThat(overtimeAccount.isAllowed()).isTrue();
        assertThat(overtimeAccount.getMaxAllowedOvertime()).hasValue(Duration.ofHours(100L));
    }

    private void ensureWorkingTime(UserIdComposite userIdComposite) {

        final List<WorkingTime> workingTimes = doWithTenant(
            () -> workingTimeService.getAllWorkingTimesByUser(userIdComposite.localId())
        );

        assertThat(workingTimes)
            .hasSize(1)
            .first().satisfies(workingTime -> {
                assertThat(workingTime.validFrom()).isEmpty();
                assertThat(workingTime.getMonday().duration()).hasHours(8L);
                assertThat(workingTime.getTuesday().duration()).hasHours(8L);
                assertThat(workingTime.getWednesday().duration()).hasHours(8L);
                assertThat(workingTime.getThursday().duration()).hasHours(8L);
                assertThat(workingTime.getFriday().duration()).hasHours(8L);
                assertThat(workingTime.getSaturday().duration()).hasHours(0L);
                assertThat(workingTime.getSunday().duration()).hasHours(0L);
            });
    }

    private void ensureTimeClocks() {

        final List<TimeClock> actualImportedTimeClocks = doWithTenant(
            () -> timeClockService.findAllTimeClocks(EXTERNAL_USER_ID)
        );

        assertThat(actualImportedTimeClocks).hasSize(3);

        assertThat(actualImportedTimeClocks.get(0)).satisfies(timeClock -> {
            assertThat(timeClock.comment()).isEqualTo("my comment");
            assertThat(timeClock.startedAt()).isEqualTo(ZonedDateTime.parse("2024-06-01T08:00:33.213+02:00[Europe/Berlin]"));
            assertThat(timeClock.stoppedAt()).hasValue(ZonedDateTime.parse("2024-06-01T12:00:00.213+02:00[Europe/Berlin]"));
            assertThat(timeClock.isBreak()).isFalse();
        });
        assertThat(actualImportedTimeClocks.get(1)).satisfies(timeClock -> {
            assertThat(timeClock.comment()).isEqualTo("my comment");
            assertThat(timeClock.startedAt()).isEqualTo(ZonedDateTime.parse("2024-06-02T12:00:00.123+02:00[Europe/Berlin]"));
            assertThat(timeClock.stoppedAt()).hasValue(ZonedDateTime.parse("2024-06-02T13:00:00.213+02:00[Europe/Berlin]"));
            assertThat(timeClock.isBreak()).isTrue();
        });
        assertThat(actualImportedTimeClocks.get(2)).satisfies(timeClock -> {
            assertThat(timeClock.comment()).isEqualTo("my comment");
            assertThat(timeClock.startedAt()).isEqualTo(ZonedDateTime.parse("2024-06-03T08:00:00.123+02:00[Europe/Berlin]"));
            assertThat(timeClock.stoppedAt()).isEmpty();
            assertThat(timeClock.isBreak()).isFalse();
        });
    }

    private void ensureTimeEntries(UserIdComposite userIdComposite) {

        final LocalDate from = LocalDate.of(2024, 6, 1);
        final LocalDate toExclusive = LocalDate.of(2024, 6, 4);

        final List<TimeEntry> timeEntries = doWithTenant(() -> timeEntryService.getEntries(from, toExclusive, userIdComposite.id()));
        assertThat(timeEntries).satisfiesExactlyInAnyOrder(
            timeEntry -> {
                assertThat(timeEntry.start()).isEqualTo(ZonedDateTime.parse("2024-06-01T08:00+02:00[Europe/Berlin]"));
                assertThat(timeEntry.end()).isEqualTo(ZonedDateTime.parse("2024-06-01T12:00+02:00[Europe/Berlin]"));
                assertThat(timeEntry.comment()).isEqualTo("dies");
                assertThat(timeEntry.isBreak()).isFalse();
            },
            timeEntry -> {
                assertThat(timeEntry.start()).isEqualTo(ZonedDateTime.parse("2024-06-01T12:00+02:00[Europe/Berlin]"));
                assertThat(timeEntry.end()).isEqualTo(ZonedDateTime.parse("2024-06-01T13:00+02:00[Europe/Berlin]"));
                assertThat(timeEntry.comment()).isEqualTo("das");
                assertThat(timeEntry.isBreak()).isFalse();
            },
            timeEntry -> {
                assertThat(timeEntry.start()).isEqualTo(ZonedDateTime.parse("2024-06-01T13:00+02:00[Europe/Berlin]"));
                assertThat(timeEntry.end()).isEqualTo(ZonedDateTime.parse("2024-06-01T17:00+02:00[Europe/Berlin]"));
                assertThat(timeEntry.comment()).isEqualTo("ananas");
                assertThat(timeEntry.isBreak()).isFalse();
            }
        );
    }

    private <T> T doWithTenant(Supplier<T> supplier) {
        try {
            tenantContextHolder.setTenantId(new TenantId(TENANT_ID));
            return supplier.get();
        } finally {
            tenantContextHolder.clear();
        }
    }
}
