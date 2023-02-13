package de.focusshift.zeiterfassung.usermanagement;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkingTimeTest {

    @Test
    void ensureGetWorkingDaysReturnsDaysWithDurationNotZero() {

        final List<WorkDay> all = WorkingTime.builder()
            .monday(1)
            .tuesday(1)
            .wednesday(1)
            .thursday(1)
            .friday(1)
            .saturday(1)
            .sunday(1)
            .build().getWorkingDays();

        final List<WorkDay> noMonday = WorkingTime.builder()
            .monday(0)
            .tuesday(1)
            .wednesday(1)
            .thursday(1)
            .friday(1)
            .saturday(1)
            .sunday(1)
            .build().getWorkingDays();

        final List<WorkDay> noTuesday = WorkingTime.builder()
            .monday(1)
            .tuesday(0)
            .wednesday(1)
            .thursday(1)
            .friday(1)
            .saturday(1)
            .sunday(1)
            .build().getWorkingDays();

        final List<WorkDay> notWednesday = WorkingTime.builder()
            .monday(1)
            .tuesday(1)
            .wednesday(0)
            .thursday(1)
            .friday(1)
            .saturday(1)
            .sunday(1)
            .build().getWorkingDays();

        final List<WorkDay> noThursday = WorkingTime.builder()
            .monday(1)
            .tuesday(1)
            .wednesday(1)
            .thursday(0)
            .friday(1)
            .saturday(1)
            .sunday(1)
            .build().getWorkingDays();

        final List<WorkDay> noFriday = WorkingTime.builder()
            .monday(1)
            .tuesday(1)
            .wednesday(1)
            .thursday(1)
            .friday(0)
            .saturday(1)
            .sunday(1)
            .build().getWorkingDays();
        final List<WorkDay> noSaturday = WorkingTime.builder()
            .monday(1)
            .tuesday(1)
            .wednesday(1)
            .thursday(1)
            .friday(1)
            .saturday(0)
            .sunday(1)
            .build().getWorkingDays();

        final List<WorkDay> noSunday = WorkingTime.builder()
            .monday(1)
            .tuesday(1)
            .wednesday(1)
            .thursday(1)
            .friday(1)
            .saturday(1)
            .sunday(0)
            .build().getWorkingDays();

        assertThat(all).contains(
            WorkDay.monday(Duration.ofHours(1)),
            WorkDay.tuesday(Duration.ofHours(1)),
            WorkDay.wednesday(Duration.ofHours(1)),
            WorkDay.thursday(Duration.ofHours(1)),
            WorkDay.friday(Duration.ofHours(1)),
            WorkDay.saturday(Duration.ofHours(1)),
            WorkDay.sunday(Duration.ofHours(1))
        );
        assertThat(noMonday).contains(
            WorkDay.tuesday(Duration.ofHours(1)),
            WorkDay.wednesday(Duration.ofHours(1)),
            WorkDay.thursday(Duration.ofHours(1)),
            WorkDay.friday(Duration.ofHours(1)),
            WorkDay.saturday(Duration.ofHours(1)),
            WorkDay.sunday(Duration.ofHours(1))
        );
        assertThat(noTuesday).contains(
            WorkDay.monday(Duration.ofHours(1)),
            WorkDay.wednesday(Duration.ofHours(1)),
            WorkDay.thursday(Duration.ofHours(1)),
            WorkDay.friday(Duration.ofHours(1)),
            WorkDay.saturday(Duration.ofHours(1)),
            WorkDay.sunday(Duration.ofHours(1))
        );
        assertThat(notWednesday).contains(
            WorkDay.monday(Duration.ofHours(1)),
            WorkDay.tuesday(Duration.ofHours(1)),
            WorkDay.thursday(Duration.ofHours(1)),
            WorkDay.friday(Duration.ofHours(1)),
            WorkDay.saturday(Duration.ofHours(1)),
            WorkDay.sunday(Duration.ofHours(1))
        );
        assertThat(noThursday).contains(
            WorkDay.monday(Duration.ofHours(1)),
            WorkDay.tuesday(Duration.ofHours(1)),
            WorkDay.wednesday(Duration.ofHours(1)),
            WorkDay.friday(Duration.ofHours(1)),
            WorkDay.saturday(Duration.ofHours(1)),
            WorkDay.sunday(Duration.ofHours(1))
        );
        assertThat(noFriday).contains(
            WorkDay.monday(Duration.ofHours(1)),
            WorkDay.tuesday(Duration.ofHours(1)),
            WorkDay.wednesday(Duration.ofHours(1)),
            WorkDay.thursday(Duration.ofHours(1)),
            WorkDay.saturday(Duration.ofHours(1)),
            WorkDay.sunday(Duration.ofHours(1))
        );
        assertThat(noSaturday).contains(
            WorkDay.monday(Duration.ofHours(1)),
            WorkDay.tuesday(Duration.ofHours(1)),
            WorkDay.wednesday(Duration.ofHours(1)),
            WorkDay.thursday(Duration.ofHours(1)),
            WorkDay.friday(Duration.ofHours(1)),
            WorkDay.sunday(Duration.ofHours(1))
        );
        assertThat(noSunday).contains(
            WorkDay.monday(Duration.ofHours(1)),
            WorkDay.tuesday(Duration.ofHours(1)),
            WorkDay.wednesday(Duration.ofHours(1)),
            WorkDay.thursday(Duration.ofHours(1)),
            WorkDay.friday(Duration.ofHours(1)),
            WorkDay.saturday(Duration.ofHours(1))
        );
    }

    @Test
    void ensureHasDifferentWorkingHours() {

        final WorkingTime differentWorkingHours = WorkingTime.builder().monday(1).tuesday(2).build();
        assertThat(differentWorkingHours.hasDifferentWorkingHours()).isTrue();

        final WorkingTime notDifferentWorkingHours = WorkingTime.builder().monday(1).build();
        assertThat(notDifferentWorkingHours.hasDifferentWorkingHours()).isFalse();

    }
}
