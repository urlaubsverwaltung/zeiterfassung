package de.focusshift.zeiterfassung.workingtime;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkingTimeCalendarServiceImplTest {

    private WorkingTimeCalendarServiceImpl sut;

    @Mock
    private WorkingTimeService workingTimeService;

    @BeforeEach
    void setUp() {
        sut = new WorkingTimeCalendarServiceImpl(workingTimeService);
    }

    @Nested
    class GetWorkingTimeCalendarForAllUsers {
        @Test
        void ensureGetWorkingTimesForAll() {

            final UserId userId_1 = new UserId("uuid-1");
            final UserLocalId userLocalId_1 = new UserLocalId(1L);
            final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);

            final UserId userId_2 = new UserId("uuid-2");
            final UserLocalId userLocalId_2 = new UserLocalId(2L);
            final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);

            final LocalDate from = LocalDate.of(2023, 2, 13);
            final LocalDate toExclusive = LocalDate.of(2023, 2, 20);

            when(workingTimeService.getAllWorkingTimes(from, toExclusive)).thenReturn(Map.of(
                userIdComposite_1, List.of(
                    WorkingTime.builder(userIdComposite_1, new WorkingTimeId(UUID.randomUUID()))
                        .monday(1)
                        .tuesday(2)
                        .wednesday(3)
                        .thursday(4)
                        .friday(5)
                        .saturday(6)
                        .sunday(7)
                        .build()
                ),
                userIdComposite_2, List.of(
                    WorkingTime.builder(userIdComposite_2, new WorkingTimeId(UUID.randomUUID()))
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
                .containsEntry(userIdComposite_1, new WorkingTimeCalendar(Map.of(
                    LocalDate.of(2023, 2, 13), new PlannedWorkingHours(Duration.ofHours(1)),
                    LocalDate.of(2023, 2, 14), new PlannedWorkingHours(Duration.ofHours(2)),
                    LocalDate.of(2023, 2, 15), new PlannedWorkingHours(Duration.ofHours(3)),
                    LocalDate.of(2023, 2, 16), new PlannedWorkingHours(Duration.ofHours(4)),
                    LocalDate.of(2023, 2, 17), new PlannedWorkingHours(Duration.ofHours(5)),
                    LocalDate.of(2023, 2, 18), new PlannedWorkingHours(Duration.ofHours(6)),
                    LocalDate.of(2023, 2, 19), new PlannedWorkingHours(Duration.ofHours(7))
                )))
                .containsEntry(userIdComposite_2, new WorkingTimeCalendar(Map.of(
                    LocalDate.of(2023, 2, 13), new PlannedWorkingHours(Duration.ofHours(7)),
                    LocalDate.of(2023, 2, 14), new PlannedWorkingHours(Duration.ofHours(6)),
                    LocalDate.of(2023, 2, 15), new PlannedWorkingHours(Duration.ofHours(5)),
                    LocalDate.of(2023, 2, 16), new PlannedWorkingHours(Duration.ofHours(4)),
                    LocalDate.of(2023, 2, 17), new PlannedWorkingHours(Duration.ofHours(3)),
                    LocalDate.of(2023, 2, 18), new PlannedWorkingHours(Duration.ofHours(2)),
                    LocalDate.of(2023, 2, 19), new PlannedWorkingHours(Duration.ofHours(1))
                )));
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

            when(workingTimeService.getAllWorkingTimes(monday, mondayNextWeek)).thenReturn(Map.of(
                userIdComposite, List.of(
                    WorkingTime.builder(userIdComposite, new WorkingTimeId(UUID.randomUUID()))
                        .validFrom(wednesday)
                        .monday(0)
                        .tuesday(0)
                        .wednesday(0)
                        .thursday(0)
                        .friday(8)
                        .build(),
                    WorkingTime.builder(userIdComposite, new WorkingTimeId(UUID.randomUUID()))
                        .validFrom(null)
                        .validTo(wednesday.minusDays(1))
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
    }

    @Nested
    class GetWorkingTimeCalendarForUsers {
        @Test
        void ensureGetWorkingTimesForUsers() {

            final UserId userId_1 = new UserId("uuid-1");
            final UserLocalId userLocalId_1 = new UserLocalId(1L);
            final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);

            final UserId userId_2 = new UserId("uuid-2");
            final UserLocalId userLocalId_2 = new UserLocalId(2L);
            final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);

            final LocalDate from = LocalDate.of(2023, 2, 13);
            final LocalDate toExclusive = from.plusWeeks(1);

            when(workingTimeService.getWorkingTimesByUsers(from, toExclusive, List.of(userLocalId_1, userLocalId_2)))
                .thenReturn(Map.of(
                    userIdComposite_1, List.of(
                        WorkingTime.builder(userIdComposite_1, new WorkingTimeId(UUID.randomUUID()))
                            .monday(1)
                            .tuesday(2)
                            .wednesday(3)
                            .thursday(4)
                            .friday(5)
                            .saturday(6)
                            .sunday(7)
                            .build()
                    ),
                    userIdComposite_2, List.of(
                        WorkingTime.builder(userIdComposite_2, new WorkingTimeId(UUID.randomUUID()))
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

            final Map<UserIdComposite, WorkingTimeCalendar> actual = sut.getWorkingTimeCalendarForUsers(from, toExclusive, List.of(userLocalId_1, userLocalId_2));

            assertThat(actual)
                .hasSize(2)
                .containsEntry(userIdComposite_1, new WorkingTimeCalendar(Map.of(
                    LocalDate.of(2023, 2, 13), new PlannedWorkingHours(Duration.ofHours(1)),
                    LocalDate.of(2023, 2, 14), new PlannedWorkingHours(Duration.ofHours(2)),
                    LocalDate.of(2023, 2, 15), new PlannedWorkingHours(Duration.ofHours(3)),
                    LocalDate.of(2023, 2, 16), new PlannedWorkingHours(Duration.ofHours(4)),
                    LocalDate.of(2023, 2, 17), new PlannedWorkingHours(Duration.ofHours(5)),
                    LocalDate.of(2023, 2, 18), new PlannedWorkingHours(Duration.ofHours(6)),
                    LocalDate.of(2023, 2, 19), new PlannedWorkingHours(Duration.ofHours(7))
                )))
                .containsEntry(userIdComposite_2, new WorkingTimeCalendar(Map.of(
                    LocalDate.of(2023, 2, 13), new PlannedWorkingHours(Duration.ofHours(7)),
                    LocalDate.of(2023, 2, 14), new PlannedWorkingHours(Duration.ofHours(6)),
                    LocalDate.of(2023, 2, 15), new PlannedWorkingHours(Duration.ofHours(5)),
                    LocalDate.of(2023, 2, 16), new PlannedWorkingHours(Duration.ofHours(4)),
                    LocalDate.of(2023, 2, 17), new PlannedWorkingHours(Duration.ofHours(3)),
                    LocalDate.of(2023, 2, 18), new PlannedWorkingHours(Duration.ofHours(2)),
                    LocalDate.of(2023, 2, 19), new PlannedWorkingHours(Duration.ofHours(1))
                )));
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

            when(workingTimeService.getWorkingTimesByUsers(monday, mondayNextWeek, List.of(userLocalId)))
                .thenReturn(Map.of(
                    userIdComposite, List.of(
                        WorkingTime.builder(userIdComposite, new WorkingTimeId(UUID.randomUUID()))
                            .validFrom(wednesday)
                            .monday(0)
                            .tuesday(0)
                            .wednesday(0)
                            .thursday(0)
                            .friday(8)
                            .build(),
                        WorkingTime.builder(userIdComposite, new WorkingTimeId(UUID.randomUUID()))
                            .validFrom(null)
                            .validTo(wednesday.minusDays(1))
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
