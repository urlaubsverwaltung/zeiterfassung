package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.AbsenceService;
import de.focusshift.zeiterfassung.publicholiday.PublicHoliday;
import de.focusshift.zeiterfassung.publicholiday.PublicHolidayCalendar;
import de.focusshift.zeiterfassung.publicholiday.PublicHolidaysService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static de.focusshift.zeiterfassung.absence.AbsenceColor.RED;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static de.focusshift.zeiterfassung.absence.DayLength.FULL;
import static de.focusshift.zeiterfassung.absence.DayLength.MORNING;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BAYERN;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.NONE;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkingTimeCalendarServiceImplTest {

    private WorkingTimeCalendarServiceImpl sut;

    @Mock
    private WorkingTimeService workingTimeService;
    @Mock
    private PublicHolidaysService publicHolidaysService;
    @Mock
    private AbsenceService absenceService;

    @BeforeEach
    void setUp() {
        sut = new WorkingTimeCalendarServiceImpl(workingTimeService, publicHolidaysService, absenceService);
    }

    @Nested
    class GetWorkingTimeCalendarForAllUsers {
        @Test
        void ensureGetWorkingTimesForAll() {

            final UserId userIdOne = new UserId("uuid-1");
            final UserLocalId userLocalIdOne = new UserLocalId(1L);
            final UserIdComposite userIdCompositeOne = new UserIdComposite(userIdOne, userLocalIdOne);

            final UserId userIdTwo = new UserId("uuid-2");
            final UserLocalId userLocalIdTwo = new UserLocalId(2L);
            final UserIdComposite userIdCompositeTwo = new UserIdComposite(userIdTwo, userLocalIdTwo);

            final LocalDate from = LocalDate.of(2023, 2, 13);
            final LocalDate toExclusive = LocalDate.of(2023, 2, 20);

            when(absenceService.getAbsencesByUserIds(any(), any(), any())).thenReturn(Map.of(userIdCompositeOne, List.of(), userIdCompositeTwo, List.of()));
            when(workingTimeService.getAllWorkingTimes(from, toExclusive)).thenReturn(Map.of(
                userIdCompositeOne, List.of(
                    WorkingTime.builder(userIdCompositeOne, new WorkingTimeId(UUID.randomUUID()))
                        .federalState(NONE)
                        .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                        .monday(1)
                        .tuesday(2)
                        .wednesday(3)
                        .thursday(4)
                        .friday(5)
                        .saturday(6)
                        .sunday(7)
                        .build()
                ),
                userIdCompositeTwo, List.of(
                    WorkingTime.builder(userIdCompositeTwo, new WorkingTimeId(UUID.randomUUID()))
                        .federalState(NONE)
                        .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                        .monday(7)
                        .tuesday(6)
                        .wednesday(5)
                        .thursday(4)
                        .friday(3)
                        .saturday(2)
                        .sunday(1)
                        .build()
                )
            ));

            final Map<UserIdComposite, WorkingTimeCalendar> actual = sut.getWorkingTimeCalendarForAllUsers(from, toExclusive);

            assertThat(actual)
                .hasSize(2)
                .containsEntry(userIdCompositeOne, new WorkingTimeCalendar(Map.of(
                    LocalDate.of(2023, 2, 13), new PlannedWorkingHours(Duration.ofHours(1)),
                    LocalDate.of(2023, 2, 14), new PlannedWorkingHours(Duration.ofHours(2)),
                    LocalDate.of(2023, 2, 15), new PlannedWorkingHours(Duration.ofHours(3)),
                    LocalDate.of(2023, 2, 16), new PlannedWorkingHours(Duration.ofHours(4)),
                    LocalDate.of(2023, 2, 17), new PlannedWorkingHours(Duration.ofHours(5)),
                    LocalDate.of(2023, 2, 18), new PlannedWorkingHours(Duration.ofHours(6)),
                    LocalDate.of(2023, 2, 19), new PlannedWorkingHours(Duration.ofHours(7))
                ), Map.of()))
                .containsEntry(userIdCompositeTwo, new WorkingTimeCalendar(Map.of(
                    LocalDate.of(2023, 2, 13), new PlannedWorkingHours(Duration.ofHours(7)),
                    LocalDate.of(2023, 2, 14), new PlannedWorkingHours(Duration.ofHours(6)),
                    LocalDate.of(2023, 2, 15), new PlannedWorkingHours(Duration.ofHours(5)),
                    LocalDate.of(2023, 2, 16), new PlannedWorkingHours(Duration.ofHours(4)),
                    LocalDate.of(2023, 2, 17), new PlannedWorkingHours(Duration.ofHours(3)),
                    LocalDate.of(2023, 2, 18), new PlannedWorkingHours(Duration.ofHours(2)),
                    LocalDate.of(2023, 2, 19), new PlannedWorkingHours(Duration.ofHours(1))
                ), Map.of()));
        }

        @Test
        void ensureGetWorkingTimesForAllConsidersPublicHolidays() {

            final UserId userIdOne = new UserId("uuid-1");
            final UserLocalId userLocalIdOne = new UserLocalId(1L);
            final UserIdComposite userIdCompositeOne = new UserIdComposite(userIdOne, userLocalIdOne);

            final UserId userIdTwo = new UserId("uuid-2");
            final UserLocalId userLocalIdTwo = new UserLocalId(2L);
            final UserIdComposite userIdCompositeTwo = new UserIdComposite(userIdTwo, userLocalIdTwo);

            final LocalDate from = LocalDate.of(2023, 2, 13);
            final LocalDate toExclusive = LocalDate.of(2023, 2, 20);

            when(absenceService.getAbsencesByUserIds(any(), any(), any())).thenReturn(Map.of(userIdCompositeOne, List.of(), userIdCompositeTwo, List.of()));
            when(workingTimeService.getAllWorkingTimes(from, toExclusive)).thenReturn(Map.of(
                userIdCompositeOne, List.of(
                    WorkingTime.builder(userIdCompositeOne, new WorkingTimeId(UUID.randomUUID()))
                        .federalState(GERMANY_BADEN_WUERTTEMBERG)
                        .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                        .monday(8)
                        .tuesday(8)
                        .build()
                ),
                userIdCompositeTwo, List.of(
                    WorkingTime.builder(userIdCompositeTwo, new WorkingTimeId(UUID.randomUUID()))
                        .federalState(GERMANY_BAYERN)
                        .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                        .monday(8)
                        .tuesday(8)
                        .build()
                )
            ));

            final PublicHolidayCalendar publicHolidayCalendar_bw = new PublicHolidayCalendar(GERMANY_BADEN_WUERTTEMBERG,
                Map.of(LocalDate.of(2023, 2, 13), List.of(new PublicHoliday(LocalDate.of(2023, 2, 13), locale -> ""))));

            final PublicHolidayCalendar publicHolidayCalendar_ba = new PublicHolidayCalendar(GERMANY_BAYERN,
                Map.of(LocalDate.of(2023, 2, 14), List.of(new PublicHoliday(LocalDate.of(2023, 2, 14), locale -> ""))));

            when(publicHolidaysService.getPublicHolidays(from, toExclusive, Set.of(GERMANY_BADEN_WUERTTEMBERG, GERMANY_BAYERN)))
                .thenReturn(Map.of(GERMANY_BADEN_WUERTTEMBERG, publicHolidayCalendar_bw, GERMANY_BAYERN, publicHolidayCalendar_ba));

            final Map<UserIdComposite, WorkingTimeCalendar> actual = sut.getWorkingTimeCalendarForAllUsers(from, toExclusive);

            assertThat(actual)
                .hasSize(2)
                .containsEntry(userIdCompositeOne, new WorkingTimeCalendar(Map.of(
                    LocalDate.of(2023, 2, 13), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 14), PlannedWorkingHours.EIGHT,
                    LocalDate.of(2023, 2, 15), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 16), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 17), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 18), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 19), PlannedWorkingHours.ZERO
                ), Map.of()))
                .containsEntry(userIdCompositeTwo, new WorkingTimeCalendar(Map.of(
                    LocalDate.of(2023, 2, 13), PlannedWorkingHours.EIGHT,
                    LocalDate.of(2023, 2, 14), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 15), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 16), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 17), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 18), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 19), PlannedWorkingHours.ZERO
                ), Map.of()));
        }

        @Test
        void createsCorrectWorkingTimeCalendarWhenOneWeekTouchesTwoWorkingTimes() {

            final UserId userId = new UserId("user-id");
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

            final LocalDate monday = LocalDate.of(2023, 12, 4);
            final LocalDate wednesday = LocalDate.of(2023, 12, 6);
            final LocalDate friday = LocalDate.of(2023, 12, 8);
            final LocalDate mondayNextWeek = monday.plusWeeks(1);

            when(absenceService.getAbsencesByUserIds(any(), any(), any())).thenReturn(Map.of(userIdComposite, List.of()));
            when(workingTimeService.getAllWorkingTimes(monday, mondayNextWeek)).thenReturn(Map.of(
                userIdComposite, List.of(
                    WorkingTime.builder(userIdComposite, new WorkingTimeId(UUID.randomUUID()))
                        .validFrom(wednesday)
                        .federalState(NONE)
                        .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                        .monday(0)
                        .tuesday(0)
                        .wednesday(0)
                        .thursday(0)
                        .friday(8)
                        .build(),
                    WorkingTime.builder(userIdComposite, new WorkingTimeId(UUID.randomUUID()))
                        .validFrom(null)
                        .validTo(wednesday.minusDays(1))
                        .federalState(NONE)
                        .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                        .monday(8)
                        .tuesday(8)
                        .wednesday(8)
                        .thursday(0)
                        .friday(0)
                        .build()
                )
            ));

            final Map<UserIdComposite, WorkingTimeCalendar> actual = sut.getWorkingTimeCalendarForAllUsers(monday, mondayNextWeek);
            assertThat(actual.get(userIdComposite)).satisfies(workingTimeCalendar -> {
                assertThat(workingTimeCalendar.plannedWorkingHours(monday.minusDays(1))).isEmpty();
                assertThat(workingTimeCalendar.plannedWorkingHours(monday)).hasValue(PlannedWorkingHours.EIGHT);
                assertThat(workingTimeCalendar.plannedWorkingHours(wednesday)).hasValue(PlannedWorkingHours.ZERO);
                assertThat(workingTimeCalendar.plannedWorkingHours(friday)).hasValue(PlannedWorkingHours.EIGHT);
            });
        }

        @Test
        void createWorkingTimeCalendarWithAbsences() {
            final UserId userIdOne = new UserId("uuid-1");
            final UserLocalId userLocalIdOne = new UserLocalId(1L);
            final UserIdComposite userIdCompositeOne = new UserIdComposite(userIdOne, userLocalIdOne);

            final UserId userIdTwo = new UserId("uuid-2");
            final UserLocalId userLocalIdTwo = new UserLocalId(2L);
            final UserIdComposite userIdCompositeTwo = new UserIdComposite(userIdTwo, userLocalIdTwo);

            final LocalDate from = LocalDate.of(2023, 2, 13);
            final LocalDate toExclusive = LocalDate.of(2023, 2, 20);


            Absence absenceUser1 = new Absence(userIdOne, ZonedDateTime.of(from.atTime(0, 0), UTC).toInstant(), ZonedDateTime.of(from.atTime(23, 59), UTC).toInstant(), FULL, foo -> "", RED, HOLIDAY);
            Absence absenceUser2 = new Absence(userIdTwo, ZonedDateTime.of(from.plusDays(1).atTime(0, 0), UTC).toInstant(), ZonedDateTime.of(from.plusDays(2).atTime(23, 59), UTC).toInstant(), MORNING, foo -> "", RED, HOLIDAY);
            when(absenceService.getAbsencesByUserIds(any(), any(), any())).thenReturn(Map.of(
                    userIdCompositeOne,
                    List.of(absenceUser1),
                    userIdCompositeTwo,
                    List.of(absenceUser2)
            ));
            when(workingTimeService.getAllWorkingTimes(from, toExclusive)).thenReturn(Map.of(
                    userIdCompositeOne, List.of(
                            WorkingTime.builder(userIdCompositeOne, new WorkingTimeId(UUID.randomUUID()))
                                    .federalState(NONE)
                                    .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                                    .monday(1)
                                    .tuesday(2)
                                    .wednesday(3)
                                    .thursday(4)
                                    .friday(5)
                                    .saturday(6)
                                    .sunday(7)
                                    .build()
                    ),
                    userIdCompositeTwo, List.of(
                            WorkingTime.builder(userIdCompositeTwo, new WorkingTimeId(UUID.randomUUID()))
                                    .federalState(NONE)
                                    .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                                    .monday(7)
                                    .tuesday(6)
                                    .wednesday(5)
                                    .thursday(4)
                                    .friday(3)
                                    .saturday(2)
                                    .sunday(1)
                                    .build()
                    )
            ));

            final Map<UserIdComposite, WorkingTimeCalendar> actual = sut.getWorkingTimeCalendarForAllUsers(from, toExclusive);

            assertThat(actual)
                    .hasSize(2)
                    .containsEntry(userIdCompositeOne, new WorkingTimeCalendar(Map.of(
                            LocalDate.of(2023, 2, 13), new PlannedWorkingHours(Duration.ofHours(1)),
                            LocalDate.of(2023, 2, 14), new PlannedWorkingHours(Duration.ofHours(2)),
                            LocalDate.of(2023, 2, 15), new PlannedWorkingHours(Duration.ofHours(3)),
                            LocalDate.of(2023, 2, 16), new PlannedWorkingHours(Duration.ofHours(4)),
                            LocalDate.of(2023, 2, 17), new PlannedWorkingHours(Duration.ofHours(5)),
                            LocalDate.of(2023, 2, 18), new PlannedWorkingHours(Duration.ofHours(6)),
                            LocalDate.of(2023, 2, 19), new PlannedWorkingHours(Duration.ofHours(7))
                    ), Map.of(
                            LocalDate.of(2023, 2, 13), List.of(absenceUser1)
                    )))
                    .containsEntry(userIdCompositeTwo, new WorkingTimeCalendar(Map.of(
                            LocalDate.of(2023, 2, 13), new PlannedWorkingHours(Duration.ofHours(7)),
                            LocalDate.of(2023, 2, 14), new PlannedWorkingHours(Duration.ofHours(6)),
                            LocalDate.of(2023, 2, 15), new PlannedWorkingHours(Duration.ofHours(5)),
                            LocalDate.of(2023, 2, 16), new PlannedWorkingHours(Duration.ofHours(4)),
                            LocalDate.of(2023, 2, 17), new PlannedWorkingHours(Duration.ofHours(3)),
                            LocalDate.of(2023, 2, 18), new PlannedWorkingHours(Duration.ofHours(2)),
                            LocalDate.of(2023, 2, 19), new PlannedWorkingHours(Duration.ofHours(1))
                    ), Map.of(
                            LocalDate.of(2023, 2, 14), List.of(absenceUser2),
                            LocalDate.of(2023, 2, 15), List.of(absenceUser2)
                    )));
        }
    }

    @Nested
    class GetWorkingTimeCalendarForUsers {
        @Test
        void ensureGetWorkingTimesForUsers() {

            final UserId userIdOne = new UserId("uuid-1");
            final UserLocalId userLocalIdOne = new UserLocalId(1L);
            final UserIdComposite userIdCompositeOne = new UserIdComposite(userIdOne, userLocalIdOne);

            final UserId userIdTwo = new UserId("uuid-2");
            final UserLocalId userLocalIdTwo = new UserLocalId(2L);
            final UserIdComposite userIdCompositeTwo = new UserIdComposite(userIdTwo, userLocalIdTwo);

            final LocalDate from = LocalDate.of(2023, 2, 13);
            final LocalDate toExclusive = from.plusWeeks(1);

            when(absenceService.getAbsencesByUserIds(any(), any(), any())).thenReturn(Map.of(userIdCompositeOne, List.of(), userIdCompositeTwo, List.of()));
            when(workingTimeService.getWorkingTimesByUsers(from, toExclusive, List.of(userLocalIdOne, userLocalIdTwo)))
                .thenReturn(Map.of(
                    userIdCompositeOne, List.of(
                        WorkingTime.builder(userIdCompositeOne, new WorkingTimeId(UUID.randomUUID()))
                            .federalState(NONE)
                            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                            .monday(1)
                            .tuesday(2)
                            .wednesday(3)
                            .thursday(4)
                            .friday(5)
                            .saturday(6)
                            .sunday(7)
                            .build()
                    ),
                    userIdCompositeTwo, List.of(
                        WorkingTime.builder(userIdCompositeTwo, new WorkingTimeId(UUID.randomUUID()))
                            .federalState(NONE)
                            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                            .monday(7)
                            .tuesday(6)
                            .wednesday(5)
                            .thursday(4)
                            .friday(3)
                            .saturday(2)
                            .sunday(1)
                            .build()
                    )
                ));

            final Map<UserIdComposite, WorkingTimeCalendar> actual = sut.getWorkingTimeCalendarForUsers(from, toExclusive, List.of(userLocalIdOne, userLocalIdTwo));

            assertThat(actual)
                .hasSize(2)
                .containsEntry(userIdCompositeOne, new WorkingTimeCalendar(Map.of(
                    LocalDate.of(2023, 2, 13), new PlannedWorkingHours(Duration.ofHours(1)),
                    LocalDate.of(2023, 2, 14), new PlannedWorkingHours(Duration.ofHours(2)),
                    LocalDate.of(2023, 2, 15), new PlannedWorkingHours(Duration.ofHours(3)),
                    LocalDate.of(2023, 2, 16), new PlannedWorkingHours(Duration.ofHours(4)),
                    LocalDate.of(2023, 2, 17), new PlannedWorkingHours(Duration.ofHours(5)),
                    LocalDate.of(2023, 2, 18), new PlannedWorkingHours(Duration.ofHours(6)),
                    LocalDate.of(2023, 2, 19), new PlannedWorkingHours(Duration.ofHours(7))
                ), Map.of()))
                .containsEntry(userIdCompositeTwo, new WorkingTimeCalendar(Map.of(
                    LocalDate.of(2023, 2, 13), new PlannedWorkingHours(Duration.ofHours(7)),
                    LocalDate.of(2023, 2, 14), new PlannedWorkingHours(Duration.ofHours(6)),
                    LocalDate.of(2023, 2, 15), new PlannedWorkingHours(Duration.ofHours(5)),
                    LocalDate.of(2023, 2, 16), new PlannedWorkingHours(Duration.ofHours(4)),
                    LocalDate.of(2023, 2, 17), new PlannedWorkingHours(Duration.ofHours(3)),
                    LocalDate.of(2023, 2, 18), new PlannedWorkingHours(Duration.ofHours(2)),
                    LocalDate.of(2023, 2, 19), new PlannedWorkingHours(Duration.ofHours(1))
                ), Map.of()));
        }

        @Test
        void ensureGetWorkingTimesForUsersConsidersPublicHolidays() {

            final UserId userIdOne = new UserId("uuid-1");
            final UserLocalId userLocalIdOne = new UserLocalId(1L);
            final UserIdComposite userIdCompositeOne = new UserIdComposite(userIdOne, userLocalIdOne);

            final UserId userIdTwo = new UserId("uuid-2");
            final UserLocalId userLocalIdTwo = new UserLocalId(2L);
            final UserIdComposite userIdCompositeTwo = new UserIdComposite(userIdTwo, userLocalIdTwo);

            final LocalDate from = LocalDate.of(2023, 2, 13);
            final LocalDate toExclusive = from.plusWeeks(1);

            when(absenceService.getAbsencesByUserIds(any(), any(), any())).thenReturn(Map.of(userIdCompositeOne, List.of(), userIdCompositeTwo, List.of()));
            when(workingTimeService.getWorkingTimesByUsers(from, toExclusive, List.of(userLocalIdOne, userLocalIdTwo)))
                .thenReturn(Map.of(
                    userIdCompositeOne, List.of(
                        WorkingTime.builder(userIdCompositeOne, new WorkingTimeId(UUID.randomUUID()))
                            .federalState(GERMANY_BADEN_WUERTTEMBERG)
                            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                            .monday(8)
                            .tuesday(8)
                            .build()
                    ),
                    userIdCompositeTwo, List.of(
                        WorkingTime.builder(userIdCompositeTwo, new WorkingTimeId(UUID.randomUUID()))
                            .federalState(GERMANY_BAYERN)
                            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                            .monday(8)
                            .tuesday(8)
                            .build()
                    )
                ));

            final PublicHolidayCalendar publicHolidayCalendar_bw = new PublicHolidayCalendar(GERMANY_BADEN_WUERTTEMBERG,
                Map.of(LocalDate.of(2023, 2, 13), List.of(new PublicHoliday(LocalDate.of(2023, 2, 13), locale -> ""))));

            final PublicHolidayCalendar publicHolidayCalendar_ba = new PublicHolidayCalendar(GERMANY_BAYERN,
                Map.of(LocalDate.of(2023, 2, 14), List.of(new PublicHoliday(LocalDate.of(2023, 2, 14), locale -> ""))));

            when(publicHolidaysService.getPublicHolidays(from, toExclusive, Set.of(GERMANY_BADEN_WUERTTEMBERG, GERMANY_BAYERN)))
                .thenReturn(Map.of(GERMANY_BADEN_WUERTTEMBERG, publicHolidayCalendar_bw, GERMANY_BAYERN, publicHolidayCalendar_ba));

            final Map<UserIdComposite, WorkingTimeCalendar> actual = sut.getWorkingTimeCalendarForUsers(from, toExclusive, List.of(userLocalIdOne, userLocalIdTwo));

            assertThat(actual)
                .hasSize(2)
                .containsEntry(userIdCompositeOne, new WorkingTimeCalendar(Map.of(
                    LocalDate.of(2023, 2, 13), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 14), PlannedWorkingHours.EIGHT,
                    LocalDate.of(2023, 2, 15), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 16), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 17), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 18), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 19), PlannedWorkingHours.ZERO
                ), Map.of()))
                .containsEntry(userIdCompositeTwo, new WorkingTimeCalendar(Map.of(
                    LocalDate.of(2023, 2, 13), PlannedWorkingHours.EIGHT,
                    LocalDate.of(2023, 2, 14), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 15), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 16), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 17), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 18), PlannedWorkingHours.ZERO,
                    LocalDate.of(2023, 2, 19), PlannedWorkingHours.ZERO
                ), Map.of()));
        }

        @Test
        void createsCorrectWorkingTimeCalendarWhenOneWeekTouchesTwoWorkingTimes() {

            final UserId userId = new UserId("user-id");
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

            final LocalDate monday = LocalDate.of(2023, 12, 4);
            final LocalDate wednesday = LocalDate.of(2023, 12, 6);
            final LocalDate friday = LocalDate.of(2023, 12, 8);
            final LocalDate mondayNextWeek = monday.plusWeeks(1);

            when(absenceService.getAbsencesByUserIds(any(), any(), any())).thenReturn(Map.of(userIdComposite, List.of()));
            when(workingTimeService.getWorkingTimesByUsers(monday, mondayNextWeek, List.of(userLocalId)))
                .thenReturn(Map.of(
                    userIdComposite, List.of(
                        WorkingTime.builder(userIdComposite, new WorkingTimeId(UUID.randomUUID()))
                            .validFrom(wednesday)
                            .federalState(NONE)
                            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                            .monday(0)
                            .tuesday(0)
                            .wednesday(0)
                            .thursday(0)
                            .friday(8)
                            .build(),
                        WorkingTime.builder(userIdComposite, new WorkingTimeId(UUID.randomUUID()))
                            .validFrom(null)
                            .validTo(wednesday.minusDays(1))
                            .federalState(NONE)
                            .worksOnPublicHoliday(WorksOnPublicHoliday.NO)
                            .monday(8)
                            .tuesday(8)
                            .wednesday(8)
                            .thursday(0)
                            .friday(0)
                            .build()
                    )
                ));

            final Map<UserIdComposite, WorkingTimeCalendar> actual = sut.getWorkingTimeCalendarForUsers(monday, mondayNextWeek, List.of(userLocalId));
            assertThat(actual.get(userIdComposite)).satisfies(workingTimeCalendar -> {
                assertThat(workingTimeCalendar.plannedWorkingHours(monday.minusDays(1))).isEmpty();
                assertThat(workingTimeCalendar.plannedWorkingHours(monday)).hasValue(PlannedWorkingHours.EIGHT);
                assertThat(workingTimeCalendar.plannedWorkingHours(wednesday)).hasValue(PlannedWorkingHours.ZERO);
                assertThat(workingTimeCalendar.plannedWorkingHours(friday)).hasValue(PlannedWorkingHours.EIGHT);
            });
        }
    }
}
