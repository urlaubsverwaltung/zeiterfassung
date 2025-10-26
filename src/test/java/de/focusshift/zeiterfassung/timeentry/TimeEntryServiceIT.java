package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TimeEntryServiceIT extends SingleTenantTestContainersBase {

    @Autowired
    private TimeEntryService timeEntryService;
    @Autowired
    private UserManagementService userManagementService;
    @Autowired
    private TenantUserService tenantUserService;
    @Autowired
    private TimeEntryRepository repository;

    @Test
    void ensureGetTimeEntriesConsideringTimeZone() {

        final User user = createAnyUser();

        final ZonedDateTime start1 = ZonedDateTime.parse("2025-06-04T23:00:00+00:00[UTC]");
        final ZonedDateTime end1 = ZonedDateTime.parse("2025-06-05T01:00:00+00:00[UTC]");
        final TimeEntry timeEntry1Valid = timeEntryService.createTimeEntry(user.userLocalId(), "comment", start1, end1, false);

        final ZonedDateTime start2 = ZonedDateTime.parse("2025-06-05T23:00:00+00:00[UTC]");
        final ZonedDateTime end2 = ZonedDateTime.parse("2025-06-06T03:00:00+00:00[UTC]");
        final TimeEntry timeEntry2Valid = timeEntryService.createTimeEntry(user.userLocalId(), "comment", start2, end2, false);

        final ZonedDateTime startUtc = ZonedDateTime.parse("2025-06-06T21:10:00+00:00[UTC]");
        final ZonedDateTime endUtc = ZonedDateTime.parse("2025-06-06T23:40:00+00:00[UTC]");
        final TimeEntry timeEntry3ValidUtc = timeEntryService.createTimeEntry(user.userLocalId(), "comment", startUtc, endUtc, false);

        final ZonedDateTime startInvalid = ZonedDateTime.parse("2025-06-06T23:00:00+00:00[UTC]");
        final ZonedDateTime endInvalid = ZonedDateTime.parse("2025-06-07T01:00:00+00:00[UTC]");
        timeEntryService.createTimeEntry(user.userLocalId(), "comment", startInvalid, endInvalid, false);

        // timeEntryService uses the Clock from configuration which is UTC.
        // and it has to return the created Europe/Berlin since it touches the requested date.
        final LocalDate requestedStart = LocalDate.parse("2025-06-05");
        final LocalDate requestedEndExclusive = LocalDate.parse("2025-06-07");
        final Map<UserIdComposite, List<TimeEntryDay>> actual = timeEntryService.getTimeEntryDaysForAllUsers(requestedStart, requestedEndExclusive);

        assertThat(actual.get(user.userIdComposite())).containsExactly(
            new TimeEntryDay(false, LocalDate.parse("2025-06-06"), new WorkDuration(Duration.ofHours(6).plusMinutes(30)), PlannedWorkingHours.ZERO, ShouldWorkingHours.ZERO,
                List.of(timeEntry3ValidUtc, timeEntry2Valid),
                List.of()
            ),
            new TimeEntryDay(false, LocalDate.parse("2025-06-05"), new WorkDuration(Duration.ofHours(2)), PlannedWorkingHours.ZERO, ShouldWorkingHours.ZERO,
                List.of(timeEntry1Valid),
                List.of()
            )
        );

        // just ensure all timeEntries exist
        assertThat(repository.findAll()).hasSize(4);
    }

    private User createAnyUser() {
        final String userUuid = UUID.randomUUID().toString();
        final EMailAddress userEmail = new EMailAddress("bruce@example.org");
        final List<SecurityRole> userRoles = List.of(ZEITERFASSUNG_USER);
        final TenantUser tenantUser = tenantUserService.createNewUser(userUuid, "Bruce", "Wayne", userEmail, userRoles);
        return userManagementService.findUserById(new UserId(tenantUser.id())).orElseThrow();
    }
}
