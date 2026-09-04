package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.absence.AbsenceTypeSourceId;
import de.focusshift.zeiterfassung.absence.AbsenceWrite;
import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.timeclock.TimeClock;
import de.focusshift.zeiterfassung.timeclock.TimeClockService;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TenantUserDeleteIT extends SingleTenantTestContainersBase {

    private static final AtomicLong ABSENCE_SOURCE_ID = new AtomicLong(90_000L);

    @Autowired
    private TenantUserService tenantUserService;
    @Autowired
    private TimeEntryService timeEntryService;
    @Autowired
    private TimeClockService timeClockService;
    @Autowired
    private WorkingTimeService workingTimeService;
    @Autowired
    private OvertimeAccountService overtimeAccountService;
    @Autowired
    private AbsenceWriteService absenceWriteService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> createdUserLocalIds = new ArrayList<>();

    @AfterEach
    void cleanUpCreatedUsers() {
        createdUserLocalIds.forEach(localId -> jdbcTemplate.update("DELETE FROM tenant_user WHERE id = ?", localId));
        createdUserLocalIds.clear();
    }

    @Test
    void deleteUserPermanentlyRemovesEveryTraceOfThePerson() {

        final TenantUser user = createUserWithData("Bruce", "Wayne");

        // sanity check: the person really has data in every table
        assertThat(rowsOfPerson(user)).allSatisfy((table, count) -> assertThat(count).describedAs(table).isPositive());

        tenantUserService.deleteUserPermanently(user.localId());

        assertThat(rowsOfPerson(user)).allSatisfy((table, count) -> assertThat(count).describedAs(table).isZero());
    }

    @Test
    void deleteUserPermanentlyKeepsHistoryOfOtherPeopleButAnonymisesIt() {

        final TenantUser editor = createUser("Alfred", "Pennyworth");
        final TenantUser owner = createUser("Bruce", "Wayne");

        final TimeEntry timeEntryOfOwner = timeEntryService.createTimeEntry(new UserLocalId(owner.localId()), "patrol",
            ZonedDateTime.parse("2025-06-04T21:00:00Z"), ZonedDateTime.parse("2025-06-04T23:00:00Z"), false);

        // the revision of the owners time entry was authored by the editor
        final Long revision = jdbcTemplate.queryForObject(
            "SELECT rev FROM time_entry_aud WHERE id = ?", Long.class, timeEntryOfOwner.id().value());
        jdbcTemplate.update("UPDATE revinfo SET updated_by = ? WHERE id = ?", editor.id(), revision);

        tenantUserService.deleteUserPermanently(editor.localId());

        assertThat(count("SELECT count(*) FROM revinfo WHERE id = ?", revision)).isOne();
        assertThat(jdbcTemplate.queryForObject("SELECT updated_by FROM revinfo WHERE id = ?", String.class, revision)).isNull();
        assertThat(count("SELECT count(*) FROM time_entry_aud WHERE rev = ?", revision)).isOne();
        assertThat(count("SELECT count(*) FROM time_entry WHERE owner = ?", owner.id())).isOne();
    }

    private Map<String, Long> rowsOfPerson(TenantUser user) {

        final Map<String, Long> rows = new LinkedHashMap<>();
        rows.put("tenant_user", count("SELECT count(*) FROM tenant_user WHERE id = ?", user.localId()));
        rows.put("tenant_user_authorities", count("SELECT count(*) FROM tenant_user_authorities WHERE tenant_user_id = ?", user.localId()));
        rows.put("time_entry", count("SELECT count(*) FROM time_entry WHERE owner = ?", user.id()));
        rows.put("time_entry_aud", count("SELECT count(*) FROM time_entry_aud WHERE owner = ?", user.id()));
        rows.put("time_clock", count("SELECT count(*) FROM time_clock WHERE owner = ?", user.id()));
        rows.put("working_time", count("SELECT count(*) FROM working_time WHERE user_id = ?", user.localId()));
        rows.put("overtime_account", count("SELECT count(*) FROM overtime_account WHERE user_id = ?", user.localId()));
        rows.put("user_settings", count("SELECT count(*) FROM user_settings WHERE tenant_user_local_id = ?", user.localId()));
        rows.put("absence", count("SELECT count(*) FROM absence WHERE user_id = ?", user.id()));
        return rows;
    }

    private long count(String sql, Object... args) {
        final Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        return count == null ? 0 : count;
    }

    private TenantUser createUser(String givenName, String familyName) {

        final TenantUser user = tenantUserService.createNewUser(UUID.randomUUID().toString(), givenName, familyName,
            new EMailAddress("%s@example.org".formatted(givenName.toLowerCase())), List.of(ZEITERFASSUNG_USER));

        createdUserLocalIds.add(user.localId());

        return user;
    }

    private TenantUser createUserWithData(String givenName, String familyName) {

        final TenantUser user = createUser(givenName, familyName);
        final UserLocalId userLocalId = new UserLocalId(user.localId());
        final UserId userId = new UserId(user.id());

        timeEntryService.createTimeEntry(userLocalId, "patrol",
            ZonedDateTime.parse("2025-06-04T21:00:00Z"), ZonedDateTime.parse("2025-06-04T23:00:00Z"), false);

        timeClockService.importTimeClock(new TimeClock(null, userId,
            ZonedDateTime.parse("2025-06-05T06:00:00Z"), "", false, Optional.of(ZonedDateTime.parse("2025-06-05T14:00:00Z"))));

        final EnumMap<DayOfWeek, Duration> workdays = new EnumMap<>(DayOfWeek.class);
        workdays.put(DayOfWeek.MONDAY, Duration.ofHours(8));
        workingTimeService.createWorkingTime(userLocalId, null, GERMANY_BADEN_WUERTTEMBERG, false, workdays);

        overtimeAccountService.updateOvertimeAccount(userLocalId, true, Duration.ofHours(10));

        jdbcTemplate.update("""
            INSERT INTO user_settings (tenant_id, tenant_user_local_id, locale, locale_browser_specific, theme, navigation_collapsed)
            VALUES ('default', ?, 'de', null, 'SYSTEM', false)
            """, user.localId());

        absenceWriteService.addAbsence(new AbsenceWrite(ABSENCE_SOURCE_ID.incrementAndGet(), userId,
            Instant.parse("2025-07-01T00:00:00Z"), Instant.parse("2025-07-02T00:00:00Z"), DayLength.FULL, Duration.ZERO, HOLIDAY,
            new AbsenceTypeSourceId(1000L)));

        return user;
    }
}
