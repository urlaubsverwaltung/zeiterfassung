package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.settings.SubtractBreakFromTimeEntrySettingsService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workduration.WorkDurationCalculationService;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeEntryDayServiceImplTest {

    private TimeEntryDayServiceImpl sut;

    @Mock
    private TimeEntryService timeEntryService;
    @Mock
    private TimeEntryRepository timeEntryRepository;
    @Mock
    private TimeEntryLockService timeEntryLockService;
    @Mock
    private WorkDurationCalculationService workDurationCalculationService;
    @Mock
    private UserDateService userDateService;
    @Mock
    private WorkingTimeCalendarService workingTimeCalendarService;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private UserSettingsProvider userSettingsProvider;
    @Mock
    private SubtractBreakFromTimeEntrySettingsService subtractBreakFromTimeEntrySettingsService;

    @BeforeEach
    void setUp() {
        sut = new TimeEntryDayServiceImpl(timeEntryService, timeEntryLockService, workDurationCalculationService,
            workingTimeCalendarService, userManagementService, userSettingsProvider, userDateService,
            subtractBreakFromTimeEntrySettingsService, timeEntryRepository);
    }

    @Test
    void ensureGetEntryWeekPageWithFirstDayOfMonth() {

        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1337L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final LocalDate firstDayOfWeek = LocalDate.of(2022, 1, 3);
        final LocalDate lastDayOfWeek = LocalDate.of(2022, 1, 9);
        when(userDateService.firstDayOfWeek(Year.of(2022), 1)).thenReturn(firstDayOfWeek);

        final ZonedDateTime timeEntryStart = ZonedDateTime.of(2022, 1, 4, 9, 0, 0, 0, userZoneId);
        final ZonedDateTime timeEntryEnd = ZonedDateTime.of(2022, 1, 4, 12, 0, 0, 0, userZoneId);

        final ZonedDateTime timeEntryBreakStart = ZonedDateTime.of(2022, 1, 4, 12, 0, 0, 0, userZoneId);
        final ZonedDateTime timeEntryBreakEnd = ZonedDateTime.of(2022, 1, 4, 13, 0, 0, 0, userZoneId);

        final TimeEntry timeEntry1 = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hack the planet!", timeEntryStart, timeEntryEnd, false);
        final TimeEntry timeEntry2 = new TimeEntry(new TimeEntryId(2L), userIdComposite, "deserved break", timeEntryBreakStart, timeEntryBreakEnd, true);

        when(timeEntryService.getEntries(firstDayOfWeek, lastDayOfWeek.plusDays(1), userLocalId))
            .thenReturn(List.of(timeEntry1, timeEntry2));

        when(timeEntryRepository.countAllByOwner("batman")).thenReturn(3L);

        final User batman = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(batman));

        when(workingTimeCalendarService.getWorkingTimeCalender(firstDayOfWeek, firstDayOfWeek.plusWeeks(1), userLocalId))
            .thenReturn(new WorkingTimeCalendar(Map.of(
                LocalDate.of(2022, 1, 3), PlannedWorkingHours.EIGHT,
                LocalDate.of(2022, 1, 4), PlannedWorkingHours.EIGHT,
                LocalDate.of(2022, 1, 5), PlannedWorkingHours.EIGHT,
                LocalDate.of(2022, 1, 6), PlannedWorkingHours.EIGHT,
                LocalDate.of(2022, 1, 7), PlannedWorkingHours.EIGHT,
                LocalDate.of(2022, 1, 8), PlannedWorkingHours.ZERO, // saturday
                LocalDate.of(2022, 1, 9), PlannedWorkingHours.ZERO  // sunday
            ), Map.of()));

        final WorkDuration calculatedWorkDuration = new WorkDuration(Duration.ofMinutes(42));
        when(workDurationCalculationService.calculateWorkDuration(List.of(timeEntry1, timeEntry2))).thenReturn(calculatedWorkDuration);
        when(workDurationCalculationService.calculateWorkDuration(List.of())).thenReturn(WorkDuration.ZERO);

        final TimeEntryWeekPage actual = sut.getEntryWeekPage(userLocalId, 2022, 1);

        assertThat(actual).isEqualTo(
            new TimeEntryWeekPage(
                new TimeEntryWeek(
                    firstDayOfWeek,
                    new PlannedWorkingHours(Duration.ofHours(40)),
                    List.of(
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2022, 1, 9),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.ZERO,
                            ShouldWorkingHours.ZERO,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2022, 1, 8),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.ZERO,
                            ShouldWorkingHours.ZERO,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2022, 1, 7),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2022, 1, 6),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2022, 1, 5),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2022, 1, 4),
                            calculatedWorkDuration,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(timeEntry1, timeEntry2),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            firstDayOfWeek,
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of())
                    )
                ),
                3
            )
        );
    }

    @Test
    void ensureGetEntryWeekPageWithDaysInCorrectOrder() {

        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1337L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final LocalDate firstDateOfWeek = LocalDate.of(2023, 1, 30);
        final LocalDate lastDateOfWeek = LocalDate.of(2023, 2, 5);
        when(userDateService.firstDayOfWeek(Year.of(2023), 5)).thenReturn(firstDateOfWeek);

        final ZonedDateTime firstDayOfWeekTimeEntryStart = ZonedDateTime.of(2023, 1, 30, 9, 0, 0, 0, userZoneId);
        final ZonedDateTime firstDayOfWeekTimeEntryEnd = ZonedDateTime.of(2023, 1, 30, 12, 0, 0, 0, userZoneId);

        final ZonedDateTime lastDayOfWeekTimeEntryStart = ZonedDateTime.of(2023, 2, 5, 9, 0, 0, 0, userZoneId);
        final ZonedDateTime lastDayOfWeekTimeEntryEnd = ZonedDateTime.of(2023, 2, 5, 12, 0, 0, 0, userZoneId);

        final TimeEntry timeEntry1 = new TimeEntry(new TimeEntryId(2L), userIdComposite, "hack the planet, second time!", lastDayOfWeekTimeEntryStart, lastDayOfWeekTimeEntryEnd, false);
        final TimeEntry timeEntry2 = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hack the planet!", firstDayOfWeekTimeEntryStart, firstDayOfWeekTimeEntryEnd, false);

        when(timeEntryService.getEntries(firstDateOfWeek, lastDateOfWeek.plusDays(1), userLocalId))
            .thenReturn(List.of(timeEntry1, timeEntry2));

        when(timeEntryRepository.countAllByOwner("batman")).thenReturn(6L);

        final User batman = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(batman));

        when(workingTimeCalendarService.getWorkingTimeCalender(firstDateOfWeek, firstDateOfWeek.plusWeeks(1), userLocalId))
            .thenReturn(new WorkingTimeCalendar(Map.of(
                firstDateOfWeek, PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 1, 31), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 2, 1), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 2, 2), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 2, 3), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 2, 4), PlannedWorkingHours.ZERO,
                LocalDate.of(2023, 2, 5), PlannedWorkingHours.ZERO
            ), Map.of()));

        final WorkDuration calculatedWorkDuration1 = new WorkDuration(Duration.ofMinutes(42));
        final WorkDuration calculatedWorkDuration2 = new WorkDuration(Duration.ofMinutes(1337));
        when(workDurationCalculationService.calculateWorkDuration(List.of(timeEntry1))).thenReturn(calculatedWorkDuration1);
        when(workDurationCalculationService.calculateWorkDuration(List.of(timeEntry2))).thenReturn(calculatedWorkDuration2);
        when(workDurationCalculationService.calculateWorkDuration(List.of())).thenReturn(WorkDuration.ZERO);

        final TimeEntryWeekPage actual = sut.getEntryWeekPage(userLocalId, 2023, 5);

        assertThat(actual).isEqualTo(
            new TimeEntryWeekPage(
                new TimeEntryWeek(
                    firstDateOfWeek,
                    new PlannedWorkingHours(Duration.ofHours(40)),
                    List.of(
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 2, 5),
                            calculatedWorkDuration1,
                            PlannedWorkingHours.ZERO,
                            ShouldWorkingHours.ZERO,
                            List.of(timeEntry1),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 2, 4),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.ZERO,
                            ShouldWorkingHours.ZERO,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 2, 3),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 2, 2),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 2, 1),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 1, 31),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            firstDateOfWeek,
                            calculatedWorkDuration2,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(timeEntry2),
                            List.of())
                    )
                ),
                6
            )
        );
    }

    @Test
    void ensureGetEntryWeekPageWithTimeEntryActuallyNotInWeekOfYear() {

        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1337L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final LocalDate firstDateOfWeek = LocalDate.of(2023, 6, 12);
        final LocalDate lastDateOfWeek = LocalDate.of(2023, 6, 18);
        when(userDateService.firstDayOfWeek(Year.of(2023), 24)).thenReturn(firstDateOfWeek);

        when(timeEntryService.getEntries(firstDateOfWeek, lastDateOfWeek.plusDays(1), userLocalId))
            .thenReturn(List.of());

        when(timeEntryRepository.countAllByOwner("batman")).thenReturn(6L);

        final User batman = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(batman));

        when(workingTimeCalendarService.getWorkingTimeCalender(firstDateOfWeek, firstDateOfWeek.plusWeeks(1), userLocalId))
            .thenReturn(new WorkingTimeCalendar(Map.of(
                LocalDate.of(2023, 6, 12), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 6, 13), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 6, 14), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 6, 15), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 6, 16), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 6, 17), PlannedWorkingHours.ZERO,
                LocalDate.of(2023, 6, 18), PlannedWorkingHours.ZERO
            ), Map.of()));

        when(workDurationCalculationService.calculateWorkDuration(List.of())).thenReturn(WorkDuration.ZERO);

        final TimeEntryWeekPage actual = sut.getEntryWeekPage(userLocalId, 2023, 24);

        assertThat(actual).isEqualTo(
            new TimeEntryWeekPage(
                new TimeEntryWeek(
                    firstDateOfWeek,
                    new PlannedWorkingHours(Duration.ofHours(40)),
                    List.of(
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 6, 18),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.ZERO,
                            ShouldWorkingHours.ZERO,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 6, 17),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.ZERO,
                            ShouldWorkingHours.ZERO,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 6, 16),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 6, 15),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 6, 14),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 6, 13),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of()),
                        new TimeEntryDay(
                            false,
                            LocalDate.of(2023, 6, 12),
                            WorkDuration.ZERO,
                            PlannedWorkingHours.EIGHT,
                            ShouldWorkingHours.EIGHT,
                            List.of(),
                            List.of())
                    )
                ),
                6
            )
        );
    }
}
