package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimeEntryDayTest {

    @Nested
    class WorkDuration {

        @Test
        void workDurationForTimeEntriesOnly() {
            /*
             *  ----------------------       ----------------------
             * | [work] 08:00 - 10:00 |     | [work] 11:00 - 14:00 |       5h WorkDuration
             *  ----------------------       ----------------------        0h BreakDuration
             */
            final TimeEntry workEntry1 = workEntry("2025-09-26T08:00:00Z", "2025-09-26T10:00:00Z");
            final TimeEntry workEntry2 = workEntry("2025-09-26T11:00:00Z", "2025-09-26T14:00:00Z");
            final TimeEntryDay day = timeEntryDay(LocalDate.parse("2025-09-26"), List.of(workEntry1, workEntry2));
            assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(5));
        }

        @Test
        void workDurationForTimeEntriesOnlyWithOverlap() {
            /*
             *  ----------------------
             * | [work] 08:00 - 09:00 |                 1h 30min WorkDuration
             *  ----------------------------------      0h BreakDuration
             *             | [work] 08:30 - 09:30 |
             *              ----------------------
             */
            final TimeEntry workEntry1 = workEntry("2025-09-26T08:00:00Z", "2025-09-26T09:00:00Z");
            final TimeEntry workEntry2 = workEntry("2025-09-26T08:30:00Z", "2025-09-26T09:30:00Z");
            final TimeEntryDay day = timeEntryDay(LocalDate.parse("2025-09-26"), List.of(workEntry1, workEntry2));
            assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofMinutes(90));
        }

        @Test
        void workDurationForTimeEntriesOnlyWithOverlapAndAdditionalTimeEntry() {
            /*
             *  ----------------------                    ----------------------
             * | [work] 08:00 - 09:00 |                  | [work] 10:00 - 11:00 |       2h 30min WorkDuration
             *  ----------------------------------        ----------------------        0h BreakDuration
             *             | [work] 08:30 - 09:30 |
             *              ----------------------
             */
            final TimeEntry workEntry1 = workEntry("2025-09-26T08:00:00Z", "2025-09-26T09:00:00Z");
            final TimeEntry workEntry2 = workEntry("2025-09-26T08:30:00Z", "2025-09-26T09:30:00Z");
            final TimeEntry workEntry3 = workEntry("2025-09-26T10:00:00Z", "2025-09-26T11:00:00Z");
            final TimeEntryDay day = timeEntryDay(LocalDate.parse("2025-09-26"), List.of(workEntry1, workEntry2, workEntry3));
            assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(2).plusMinutes(30));
        }

        @Test
        void workDurationForTimeEntryWithOverlappingBreak() {
            /*
             *  --------------------------------------------------
             * |            [work]  08:00 - 17:00                 |     8h WorkDuration (not 9h)
             *  --------------------------------------------------      1h BreakDuration
             *            | [break] 12:00 - 13:00 |
             *             -----------------------
             */
            final TimeEntry workEntry = workEntry("2025-09-26T08:00:00Z", "2025-09-26T17:00:00Z");
            final TimeEntry breakEntry = breakEntry("2025-09-26T12:00:00Z", "2025-09-26T13:00:00Z");
            final TimeEntryDay day = timeEntryDay(LocalDate.parse("2025-09-26"), List.of(workEntry, breakEntry));
            assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(8));
        }

        @Test
        void workDurationForTimeEntriesWithOverlappingBreak() {
            /*
             *  ----------------------        ----------------------
             * | [work] 08:00 - 12:15 |      | [work] 12:45 - 17:00 |       7h 45min WorkDuration (not 8h 30min)
             *  ----------------------------------------------------        1h 15min BreakDuration
             *                  | [break] 12:00 - 13:15 |
             *                   -----------------------
             */
            final TimeEntry workEntry1 = workEntry("2025-09-26T08:00:00Z", "2025-09-26T12:15:00Z");
            final TimeEntry workEntry2 = workEntry("2025-09-26T12:45:00Z", "2025-09-26T17:00:00Z");
            final TimeEntry breakEntry = breakEntry("2025-09-26T12:00:00Z", "2025-09-26T13:15:00Z");
            final TimeEntryDay day = timeEntryDay(LocalDate.parse("2025-09-26"), List.of(workEntry1, workEntry2, breakEntry));
            assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(7).plusMinutes(45));
        }

        @Test
        void workDurationForTimeEntriesOverlappingAndBreakOverlapping() {
            /*
             *  ----------------------                    ----------------------
             * | [work] 08:00 - 09:00 |                  | [work] 10:00 - 11:00 |       1h 30min WorkDuration (not 2h 30min)
             *  ----------------------------------        ----------------------        1h BreakDuration
             *             | [work] 08:30 - 09:30 |
             *        ----------------------------
             *       | [break] 08:15 - 09:15 |
             *        -----------------------
             */
            final TimeEntry workEntry1 = workEntry("2025-09-26T08:00:00Z", "2025-09-26T09:00:00Z");
            final TimeEntry workEntry2 = workEntry("2025-09-26T08:30:00Z", "2025-09-26T09:30:00Z");
            final TimeEntry workEntry3 = workEntry("2025-09-26T10:00:00Z", "2025-09-26T11:00:00Z");
            final TimeEntry breakEntry = breakEntry("2025-09-26T08:15:00Z", "2025-09-26T09:15:00Z");
            final TimeEntryDay day = timeEntryDay(LocalDate.parse("2025-09-26"), List.of(workEntry1, workEntry2, workEntry3, breakEntry));
            assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(1).plusMinutes(30));
        }

        @Test
        void workDurationForTimeEntriesOverlappingAndBreakOverlappingBoth() {
            /*
             *  ----------------------                    ----------------------
             * | [work] 08:00 - 09:00 |                  | [work] 10:00 - 11:00 |       1h 15min WorkDuration (not 2h 30min)
             *  ----------------------------------        ----------------------        1h 30min BreakDuration
             *             | [work] 08:30 - 09:30 |
             *        --------------------------------
             *       | [break] 08:15 - 09:45          |
             *        --------------------------------
             */
            final TimeEntry workEntry1 = workEntry("2025-09-26T08:00:00Z", "2025-09-26T09:00:00Z");
            final TimeEntry workEntry2 = workEntry("2025-09-26T08:30:00Z", "2025-09-26T09:30:00Z");
            final TimeEntry workEntry3 = workEntry("2025-09-26T10:00:00Z", "2025-09-26T11:00:00Z");
            final TimeEntry breakEntry = breakEntry("2025-09-26T08:15:00Z", "2025-09-26T09:45:00Z");
            final TimeEntryDay day = timeEntryDay(LocalDate.parse("2025-09-26"), List.of(workEntry1, workEntry2, workEntry3, breakEntry));
            assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(1).plusMinutes(15));
        }


        @Test
        void workDurationForTimeEntriesWithMultipleOverlappingBreak() {
            /*
             *  -------------------------------------------------------------------
             * | [work] 08:00 - 17:00                                              |      7h 30min WorkDuration (not 9h)
             *  -------------------------------------------------------------------       1h 30min BreakDuration
             *                  | [break] 12:00 - 13:00 |
             *                   ---------------------------------
             *                            | [break] 12:30 - 13:30 |
             *                             -----------------------
             */
            final TimeEntry workEntry = workEntry("2025-09-26T08:00:00Z", "2025-09-26T17:00:00Z");
            final TimeEntry breakEntry1 = breakEntry("2025-09-26T12:00:00Z", "2025-09-26T13:00:00Z");
            final TimeEntry breakEntry2 = breakEntry("2025-09-26T12:30:00Z", "2025-09-26T13:30:00Z");
            final TimeEntryDay day = timeEntryDay(LocalDate.parse("2025-09-26"), List.of(workEntry, breakEntry1, breakEntry2));
            assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(7).plusMinutes(30));
        }

        @Test
        void workDurationForTimeEntryWithOverlappingBreakAtStart() {
            /*
             *               --------------------------------------
             *              | [work]  08:00 - 17:00                |     8h WorkDuration (not 9h)
             *  ---------------------------------------------------      2h BreakDuration
             * | [break] 07:00 - 09:00 |
             *  -----------------------
             */
            final TimeEntry workEntry = workEntry("2025-09-26T08:00:00Z", "2025-09-26T17:00:00Z");
            final TimeEntry breakEntry = breakEntry("2025-09-26T07:00:00Z", "2025-09-26T09:00:00Z");
            final TimeEntryDay day = timeEntryDay(LocalDate.parse("2025-09-26"), List.of(workEntry, breakEntry));
            assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(8));
        }

        @Test
        void workDurationForTimeEntryWithOverlappingBreakAtEnd() {
            /*
             *  --------------------------------------
             * | [work]  08:00 - 17:00                |                     8h WorkDuration (not 9h)
             *  -----------------------------------------------------       2h BreakDuration
             *                               | [break] 16:00 - 18:00 |
             *                                -----------------------
             */
            final TimeEntry workEntry = workEntry("2025-09-26T08:00:00Z", "2025-09-26T17:00:00Z");
            final TimeEntry breakEntry = breakEntry("2025-09-26T16:00:00Z", "2025-09-26T18:00:00Z");
            final TimeEntryDay day = timeEntryDay(LocalDate.parse("2025-09-26"), List.of(workEntry, breakEntry));
            assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(8));
        }

        private TimeEntryDay timeEntryDay(LocalDate date, List<TimeEntry> timeEntries) {
            return new TimeEntryDay(false, date, PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, timeEntries, List.of());
        }

        private static TimeEntry workEntry(String start, String end) {
            return entry(start, end, false);
        }

        private static TimeEntry breakEntry(String start, String end) {
            return entry(start, end, true);
        }

        private static TimeEntry entry(String start, String end, boolean isBreak) {
            return new TimeEntry(new TimeEntryId(1L), anyUserIdComposite(), "", ZonedDateTime.parse(start), ZonedDateTime.parse(end), isBreak);
        }
    }

    private static UserIdComposite anyUserIdComposite() {
        return new UserIdComposite(new UserId("user-id"), new UserLocalId(1L));
    }
}
