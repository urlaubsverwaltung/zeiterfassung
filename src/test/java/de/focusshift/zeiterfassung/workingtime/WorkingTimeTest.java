package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

class WorkingTimeTest {

    @Test
    void ensureActualWorkingDaysReturnsDaysWithDurationNotZero() {

        final List<DayOfWeek> all = WorkingTime.builder(anyUserIdComposite(), new WorkingTimeId(UUID.randomUUID()))
            .monday(1)
            .tuesday(1)
            .wednesday(1)
            .thursday(1)
            .friday(1)
            .saturday(1)
            .sunday(1)
            .build().actualWorkingDays();

        final List<DayOfWeek> noMonday = WorkingTime.builder(anyUserIdComposite(), new WorkingTimeId(UUID.randomUUID()))
            .monday(0)
            .tuesday(1)
            .wednesday(1)
            .thursday(1)
            .friday(1)
            .saturday(1)
            .sunday(1)
            .build().actualWorkingDays();

        final List<DayOfWeek> noTuesday = WorkingTime.builder(anyUserIdComposite(), new WorkingTimeId(UUID.randomUUID()))
            .monday(1)
            .tuesday(0)
            .wednesday(1)
            .thursday(1)
            .friday(1)
            .saturday(1)
            .sunday(1)
            .build().actualWorkingDays();

        final List<DayOfWeek> notWednesday = WorkingTime.builder(anyUserIdComposite(), new WorkingTimeId(UUID.randomUUID()))
            .monday(1)
            .tuesday(1)
            .wednesday(0)
            .thursday(1)
            .friday(1)
            .saturday(1)
            .sunday(1)
            .build().actualWorkingDays();

        final List<DayOfWeek> noThursday = WorkingTime.builder(anyUserIdComposite(), new WorkingTimeId(UUID.randomUUID()))
            .monday(1)
            .tuesday(1)
            .wednesday(1)
            .thursday(0)
            .friday(1)
            .saturday(1)
            .sunday(1)
            .build().actualWorkingDays();

        final List<DayOfWeek> noFriday = WorkingTime.builder(anyUserIdComposite(), new WorkingTimeId(UUID.randomUUID()))
            .monday(1)
            .tuesday(1)
            .wednesday(1)
            .thursday(1)
            .friday(0)
            .saturday(1)
            .sunday(1)
            .build().actualWorkingDays();

        final List<DayOfWeek> noSaturday = WorkingTime.builder(anyUserIdComposite(), new WorkingTimeId(UUID.randomUUID()))
            .monday(1)
            .tuesday(1)
            .wednesday(1)
            .thursday(1)
            .friday(1)
            .saturday(0)
            .sunday(1)
            .build().actualWorkingDays();

        final List<DayOfWeek> noSunday = WorkingTime.builder(anyUserIdComposite(), new WorkingTimeId(UUID.randomUUID()))
            .monday(1)
            .tuesday(1)
            .wednesday(1)
            .thursday(1)
            .friday(1)
            .saturday(1)
            .sunday(0)
            .build().actualWorkingDays();

        assertThat(all).containsExactly(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY);
        assertThat(noMonday).containsExactly(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY);
        assertThat(noTuesday).containsExactly(MONDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY);
        assertThat(notWednesday).containsExactly(MONDAY, TUESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY);
        assertThat(noThursday).containsExactly(MONDAY, TUESDAY, WEDNESDAY, FRIDAY, SATURDAY, SUNDAY);
        assertThat(noFriday).containsExactly(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, SATURDAY, SUNDAY);
        assertThat(noSaturday).containsExactly(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SUNDAY);
        assertThat(noSunday).containsExactly(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY);
    }

    @Test
    void ensureHasDifferentWorkingHours() {

        final WorkingTimeId id_1 = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime differentWorkingHours = WorkingTime.builder(anyUserIdComposite(), id_1).monday(1).tuesday(2).build();
        assertThat(differentWorkingHours.hasDifferentWorkingHours()).isTrue();

        final WorkingTimeId id_2 = new WorkingTimeId(UUID.randomUUID());
        final WorkingTime notDifferentWorkingHours = WorkingTime.builder(anyUserIdComposite(), id_2).monday(1).build();
        assertThat(notDifferentWorkingHours.hasDifferentWorkingHours()).isFalse();
    }

    private static UserIdComposite anyUserIdComposite() {
        return new UserIdComposite(new UserId("user-id"), new UserLocalId(1L));
    }
}
