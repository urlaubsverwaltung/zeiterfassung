package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.junit.jupiter.api.BeforeEach;
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

    @Test
    void ensureGetWorkingTimesForAll() {

        final UserId userId_1 = new UserId("uuid-1");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);

        final UserId userId_2 = new UserId("uuid-2");
        final UserLocalId userLocalId_2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);

        when(workingTimeService.getAllWorkingTimesByUsers()).thenReturn(Map.of(
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

        final LocalDate from = LocalDate.of(2023, 2, 13);
        final LocalDate toExclusive = LocalDate.of(2023, 2, 20);
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
    void ensureGetWorkingTimesForUsers() {

        final UserId userId_1 = new UserId("uuid-1");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);

        final UserId userId_2 = new UserId("uuid-2");
        final UserLocalId userLocalId_2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);

        when(workingTimeService.getWorkingTimesByUsers(List.of(userLocalId_1, userLocalId_2))).thenReturn(Map.of(
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

        final Map<UserIdComposite, WorkingTimeCalendar> actual = sut.getWorkingTimeCalendarForUsers(
            LocalDate.of(2023, 2, 13),
            LocalDate.of(2023, 2, 20),
            List.of(userLocalId_1, userLocalId_2)
        );

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
}
