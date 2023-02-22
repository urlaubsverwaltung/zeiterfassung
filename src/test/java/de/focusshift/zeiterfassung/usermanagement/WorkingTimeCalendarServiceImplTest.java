package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

        when(workingTimeService.getAllWorkingTimeByUsers()).thenReturn(Map.of(
                new UserLocalId(1L), WorkingTime.builder()
                    .userId(new UserLocalId(1L))
                    .monday(1)
                    .tuesday(2)
                    .wednesday(3)
                    .thursday(4)
                    .friday(5)
                    .saturday(6)
                    .sunday(7)
                    .build(),
                new UserLocalId(2L), WorkingTime.builder()
                    .userId(new UserLocalId(1L))
                    .monday(7)
                    .tuesday(6)
                    .wednesday(5)
                    .thursday(4)
                    .friday(3)
                    .saturday(2)
                    .sunday(1)
                    .build()
        ));

        final Map<UserLocalId, WorkingTimeCalendar> actual = sut.getWorkingTimes(LocalDate.of(2023, 2, 13), LocalDate.of(2023, 2, 20));

        assertThat(actual)
            .hasSize(2)
            .containsEntry(new UserLocalId(1L), new WorkingTimeCalendar(Map.of(
                LocalDate.of(2023, 2, 13), new PlannedWorkingHours(Duration.ofHours(1)),
                LocalDate.of(2023, 2, 14), new PlannedWorkingHours(Duration.ofHours(2)),
                LocalDate.of(2023, 2, 15), new PlannedWorkingHours(Duration.ofHours(3)),
                LocalDate.of(2023, 2, 16), new PlannedWorkingHours(Duration.ofHours(4)),
                LocalDate.of(2023, 2, 17), new PlannedWorkingHours(Duration.ofHours(5)),
                LocalDate.of(2023, 2, 18), new PlannedWorkingHours(Duration.ofHours(6)),
                LocalDate.of(2023, 2, 19), new PlannedWorkingHours(Duration.ofHours(7))
            )))
            .containsEntry(new UserLocalId(2L), new WorkingTimeCalendar(Map.of(
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

        final UserLocalId userId_2 = new UserLocalId(2L);
        final UserLocalId userId_1 = new UserLocalId(1L);

        when(workingTimeService.getWorkingTimeByUsers(List.of(userId_1, userId_2))).thenReturn(Map.of(
            userId_1, WorkingTime.builder()
                .userId(userId_1)
                .monday(1)
                .tuesday(2)
                .wednesday(3)
                .thursday(4)
                .friday(5)
                .saturday(6)
                .sunday(7)
                .build(),
            userId_2, WorkingTime.builder()
                .userId(userId_1)
                .monday(7)
                .tuesday(6)
                .wednesday(5)
                .thursday(4)
                .friday(3)
                .saturday(2)
                .sunday(1)
                .build()
        ));

        final Map<UserLocalId, WorkingTimeCalendar> actual = sut.getWorkingTimes(
            LocalDate.of(2023, 2, 13),
            LocalDate.of(2023, 2, 20),
            List.of(userId_1, userId_2)
        );

        assertThat(actual)
            .hasSize(2)
            .containsEntry(userId_1, new WorkingTimeCalendar(Map.of(
                LocalDate.of(2023, 2, 13), new PlannedWorkingHours(Duration.ofHours(1)),
                LocalDate.of(2023, 2, 14), new PlannedWorkingHours(Duration.ofHours(2)),
                LocalDate.of(2023, 2, 15), new PlannedWorkingHours(Duration.ofHours(3)),
                LocalDate.of(2023, 2, 16), new PlannedWorkingHours(Duration.ofHours(4)),
                LocalDate.of(2023, 2, 17), new PlannedWorkingHours(Duration.ofHours(5)),
                LocalDate.of(2023, 2, 18), new PlannedWorkingHours(Duration.ofHours(6)),
                LocalDate.of(2023, 2, 19), new PlannedWorkingHours(Duration.ofHours(7))
            )))
            .containsEntry(userId_2, new WorkingTimeCalendar(Map.of(
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
