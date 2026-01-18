package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.absence.AbsenceColor;
import de.focusshift.zeiterfassung.absence.AbsenceTypeCategory;
import de.focusshift.zeiterfassung.absence.AbsenceTypeService;
import de.focusshift.zeiterfassung.absence.AbsenceTypeSourceId;
import de.focusshift.zeiterfassung.absence.AbsenceTypeUpdate;
import de.focusshift.zeiterfassung.absence.AbsenceWrite;
import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.companyvacation.CompanyVacationWrite;
import de.focusshift.zeiterfassung.companyvacation.CompanyVacationWriteService;
import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class WorkingTimeCalendarIT extends SingleTenantTestContainersBase {

    @Autowired
    private WorkingTimeCalendarService sut;

    @Autowired
    private TenantUserService tenantUserService;
    @Autowired
    private WorkingTimeService workingTimeService;
    @Autowired
    private AbsenceTypeService absenceTypeService;
    @Autowired
    private AbsenceWriteService absenceWriteService;
    @Autowired
    private CompanyVacationWriteService companyVacationWriteService;

    @Test
    void ensureWorkingTimeCalendarShouldWorkingHoursWithOverlappingAbsences() {

        final TenantUser tenantUser1 = tenantUserService.createNewUser(UUID.randomUUID().toString(), "", "", new EMailAddress(""), Set.of());
        final TenantUser tenantUser2 = tenantUserService.createNewUser(UUID.randomUUID().toString(), "", "", new EMailAddress(""), Set.of());
        final TenantUser tenantUser3 = tenantUserService.createNewUser(UUID.randomUUID().toString(), "", "", new EMailAddress(""), Set.of());

        // user1 works 5h Monday to Thursday
        final UserId userId1 = new UserId(tenantUser1.id());
        final UserLocalId userLocalId1 = new UserLocalId(tenantUser1.localId());
        final UserIdComposite userIdComposite1 = new UserIdComposite(userId1, userLocalId1);

        // user2 works 8h Monday to Friday
        final UserId userId2 = new UserId(tenantUser2.id());
        final UserLocalId userLocalId2 = new UserLocalId(tenantUser2.localId());
        final UserIdComposite userIdComposite2 = new UserIdComposite(userId2, userLocalId2);

        // user3 works 8h Monday to Friday AND works on public holiday
        final UserId userId3 = new UserId(tenantUser3.id());
        final UserLocalId userLocalId3 = new UserLocalId(tenantUser3.localId());
        final UserIdComposite userIdComposite3 = new UserIdComposite(userId3, userLocalId3);

        createWorkingTimes(userLocalId1, userLocalId2, userLocalId3);

        absenceTypeService.updateAbsenceType(
            new AbsenceTypeUpdate(1L,AbsenceTypeCategory.OVERTIME, AbsenceColor.PINK, Map.of(Locale.GERMAN, "Überstundenabbau"))
        );

        // overtime reduction of user1 (spans 4 working days a 5h)
        absenceWriteService.addAbsence(new AbsenceWrite(1L, userId1,
            LocalDate.of(2025, 12, 23).atStartOfDay().toInstant(UTC),
            LocalDate.of(2026, 1, 6).atStartOfDay().toInstant(UTC),
            DayLength.FULL, Duration.ofHours(4 * 5), AbsenceTypeCategory.OVERTIME, new AbsenceTypeSourceId(1L)
        ));
        // overtime reduction of user2 (spans 5 working days a 8h)
        absenceWriteService.addAbsence(new AbsenceWrite(2L, userId2,
            LocalDate.of(2025, 12, 23).atStartOfDay().toInstant(UTC),
            LocalDate.of(2026, 1, 6).atStartOfDay().toInstant(UTC),
            DayLength.FULL, Duration.ofHours(5 * 8), AbsenceTypeCategory.OVERTIME, new AbsenceTypeSourceId(1L)
        ));
        // overtime reduction of user3 (spans 9 working days a 8h, working on public holidays)
        absenceWriteService.addAbsence(new AbsenceWrite(3L, userId3,
            LocalDate.of(2025, 12, 23).atStartOfDay().toInstant(UTC),
            LocalDate.of(2026, 1, 6).atStartOfDay().toInstant(UTC),
            DayLength.FULL, Duration.ofHours(9 * 8), AbsenceTypeCategory.OVERTIME, new AbsenceTypeSourceId(1L)
        ));

        // public holidays are integrated and provided by a lib
        createCompanyVacation();

        // request workingTimeCalendars for one week, like a report view does
        final Map<UserIdComposite, WorkingTimeCalendar> actual = sut.getWorkingTimeCalendarForUsers(
            LocalDate.of(2025, 12, 22),
            LocalDate.of(2025, 12, 29),
            List.of(userLocalId1, userLocalId2, userLocalId3)
        );

        assertThat(actual.get(userIdComposite1)).satisfies(this::assertShouldWorkingHoursIsZero);
        assertThat(actual.get(userIdComposite2)).satisfies(this::assertShouldWorkingHoursIsZero);
        assertThat(actual.get(userIdComposite3)).satisfies(this::assertShouldWorkingHoursIsZero);
    }

    private void assertShouldWorkingHoursIsZero(WorkingTimeCalendar calendar) {
        // ---------------------------------------
        // part of requested interval

        // zero -> overtime reduction
        assertThat(calendar.shouldWorkingHours(LocalDate.of(2025, 12, 23))).hasValue(ShouldWorkingHours.ZERO);
        // zero -> christmas-eve FULL
        assertThat(calendar.shouldWorkingHours(LocalDate.of(2025, 12, 24))).hasValue(ShouldWorkingHours.ZERO);
        // zero -> public holiday (user1, user2) OR overtime reduction (user3)
        assertThat(calendar.shouldWorkingHours(LocalDate.of(2025, 12, 25))).hasValue(ShouldWorkingHours.ZERO);
        // zero -> public holiday (user1, user2) OR overtime reduction (user3)
        assertThat(calendar.shouldWorkingHours(LocalDate.of(2025, 12, 26))).hasValue(ShouldWorkingHours.ZERO);
        // zero -> no workday (Saturday)
        assertThat(calendar.shouldWorkingHours(LocalDate.of(2025, 12, 27))).hasValue(ShouldWorkingHours.ZERO);
        // zero -> no workday (Sunday)
        assertThat(calendar.shouldWorkingHours(LocalDate.of(2025, 12, 28))).hasValue(ShouldWorkingHours.ZERO);

        // ---------------------------------------
        // not part of requested interval
        // however, part of data since required for calculation and therefore must have value!

        // zero -> silvester FULL (despite outside requested interval, data exists in calendar)
        assertThat(calendar.shouldWorkingHours(LocalDate.of(2025, 12, 31))).hasValue(ShouldWorkingHours.ZERO);
        // zero -> public holiday
        assertThat(calendar.shouldWorkingHours(LocalDate.of(2026, 1, 6))).hasValue(ShouldWorkingHours.ZERO);
    }

    private void createWorkingTimes(UserLocalId userLocalId1, UserLocalId userLocalId2, UserLocalId userLocalId3) {

        workingTimeService.createWorkingTime(
            userLocalId1,
            null,
            FederalState.GERMANY_BADEN_WUERTTEMBERG,
            false,
            new EnumMap<>(Map.of(
                MONDAY, Duration.ofHours(5),
                TUESDAY, Duration.ofHours(5),
                WEDNESDAY, Duration.ofHours(5),
                THURSDAY, Duration.ofHours(5),
                FRIDAY, Duration.ZERO,
                SATURDAY, Duration.ZERO,
                SUNDAY, Duration.ZERO
            ))
        );

        workingTimeService.createWorkingTime(
            userLocalId2,
            null,
            FederalState.GERMANY_BADEN_WUERTTEMBERG,
            false,
            new EnumMap<>(Map.of(
                MONDAY, Duration.ofHours(8),
                TUESDAY, Duration.ofHours(8),
                WEDNESDAY, Duration.ofHours(8),
                THURSDAY, Duration.ofHours(8),
                FRIDAY, Duration.ofHours(8),
                SATURDAY, Duration.ZERO,
                SUNDAY, Duration.ZERO
            ))
        );

        workingTimeService.createWorkingTime(
            userLocalId3,
            null,
            FederalState.GERMANY_BADEN_WUERTTEMBERG,
            true,
            new EnumMap<>(Map.of(
                MONDAY, Duration.ofHours(8),
                TUESDAY, Duration.ofHours(8),
                WEDNESDAY, Duration.ofHours(8),
                THURSDAY, Duration.ofHours(8),
                FRIDAY, Duration.ofHours(8),
                SATURDAY, Duration.ZERO,
                SUNDAY, Duration.ZERO
            ))
        );
    }

    private void createCompanyVacation() {

        companyVacationWriteService.addOrUpdateCompanyVacation(new CompanyVacationWrite(UUID.randomUUID().toString(),
            LocalDate.of(2025, 12, 24).atStartOfDay().toInstant(UTC),
            LocalDate.of(2025, 12, 24).atStartOfDay().toInstant(UTC),
            de.focusshift.zeiterfassung.companyvacation.DayLength.FULL
        ));

        companyVacationWriteService.addOrUpdateCompanyVacation(new CompanyVacationWrite(UUID.randomUUID().toString(),
            LocalDate.of(2025, 12, 31).atStartOfDay().toInstant(UTC),
            LocalDate.of(2025, 12, 31).atStartOfDay().toInstant(UTC),
            de.focusshift.zeiterfassung.companyvacation.DayLength.FULL
        ));
    }
}
