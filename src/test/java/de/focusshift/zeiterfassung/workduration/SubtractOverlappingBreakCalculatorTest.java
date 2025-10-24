package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubtractOverlappingBreakCalculatorTest {

    private SubtractOverlappingBreakCalculator sut;

    @BeforeEach
    void setUp() {
        sut = new SubtractOverlappingBreakCalculator();
    }

    @Nested
    class CalculateWorkDuration {

        @Test
        void workDurationForTimeEntriesOnly() {
            /*
             *  ----------------------       ----------------------
             * | [work] 08:00 - 10:00 |     | [work] 11:00 - 14:00 |       5h WorkDuration
             *  ----------------------       ----------------------        0h BreakDuration
             */
            final TimeEntry workEntry1 = workEntry("2025-09-26T08:00:00Z", "2025-09-26T10:00:00Z");
            final TimeEntry workEntry2 = workEntry("2025-09-26T11:00:00Z", "2025-09-26T14:00:00Z");

            final WorkDuration actual = sut.calculateWorkDuration(List.of(workEntry1, workEntry2));
            assertThat(actual.durationInMinutes()).isEqualTo(Duration.ofHours(5));
        }

        @Test
        void workDurationForTimeEntriesOnlyWithOverlapShouldNotBeSubtracted() {
            /*
             *  ----------------------
             * | [work] 08:00 - 09:00 |                 2h WorkDuration
             *  ----------------------------------      0h BreakDuration
             *             | [work] 08:30 - 09:30 |
             *              ----------------------
             */
            final TimeEntry workEntry1 = workEntry("2025-09-26T08:00:00Z", "2025-09-26T09:00:00Z");
            final TimeEntry workEntry2 = workEntry("2025-09-26T08:30:00Z", "2025-09-26T09:30:00Z");

            final WorkDuration actual = sut.calculateWorkDuration(List.of(workEntry1, workEntry2));
            assertThat(actual.durationInMinutes()).isEqualTo(Duration.ofHours(2));
        }

        @Test
        void workDurationForTimeEntriesOnlyWithOverlapAndAdditionalTimeEntryShouldNotBeSubtracted() {
            /*
             *  ----------------------                    ----------------------
             * | [work] 08:00 - 09:00 |                  | [work] 10:00 - 11:00 |       3h WorkDuration
             *  ----------------------------------        ----------------------        0h BreakDuration
             *             | [work] 08:30 - 09:30 |
             *              ----------------------
             */
            final TimeEntry workEntry1 = workEntry("2025-09-26T08:00:00Z", "2025-09-26T09:00:00Z");
            final TimeEntry workEntry2 = workEntry("2025-09-26T08:30:00Z", "2025-09-26T09:30:00Z");
            final TimeEntry workEntry3 = workEntry("2025-09-26T10:00:00Z", "2025-09-26T11:00:00Z");

            final WorkDuration actual = sut.calculateWorkDuration(List.of(workEntry1, workEntry2, workEntry3));
            assertThat(actual.durationInMinutes()).isEqualTo(Duration.ofHours(3));
        }

        @ParameterizedTest
        @CsvSource({
            /*
             *  --------------------------------------------------
             * |            [work]  08:00 - 17:00                 |     8h WorkDuration (not 9h)
             *  --------------------------------------------------      1h BreakDuration
             *            | [break] 12:00 - 13:00 |
             *             -----------------------
             */
            "2025-09-26T08:00:00Z, 2025-09-26T17:00:00Z, 2025-09-26T12:00:00Z, 2025-09-26T13:00:00Z",
            /*
             *               --------------------------------------
             *              | [work]  08:00 - 17:00                |     8h WorkDuration (not 9h)
             *  ---------------------------------------------------      2h BreakDuration
             * | [break] 07:00 - 09:00 |
             *  -----------------------
             */
            "2025-09-26T08:00:00Z, 2025-09-26T17:00:00Z, 2025-09-26T07:00:00Z, 2025-09-26T09:00:00Z",
            /*
             *  --------------------------------------
             * | [work]  08:00 - 17:00                |                     8h WorkDuration (not 9h)
             *  -----------------------------------------------------       2h BreakDuration
             *                               | [break] 16:00 - 18:00 |
             *                                -----------------------
             */
            "2025-09-26T08:00:00Z, 2025-09-26T17:00:00Z, 2025-09-26T16:00:00Z, 2025-09-26T18:00:00Z"
        })
        void ensureOverlappingWorkEntryAndBreakEntryGetSubtracted(String workFrom, String workTo, String breakFrom, String breakTo) {

            final TimeEntry workEntry = workEntry(workFrom, workTo);
            final TimeEntry breakEntry = breakEntry(breakFrom, breakTo);

            final WorkDuration actual = sut.calculateWorkDuration(List.of(workEntry, breakEntry));
            assertThat(actual.durationInMinutes()).isEqualTo(Duration.ofHours(8));
        }

        @Test
        void workDurationForTimeEntriesWithOverlappingBreakGetSubtracted() {
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

            final WorkDuration actual = sut.calculateWorkDuration(List.of(workEntry1, workEntry2, breakEntry));
            assertThat(actual.durationInMinutes()).isEqualTo(Duration.ofHours(7).plusMinutes(45));
        }

        @Test
        void workDurationForTimeEntriesOverlappingAndBreakOverlapping() {
            /*
             *  ----------------------                    ----------------------
             * | [work] 08:00 - 09:00 |                  | [work] 10:00 - 11:00 |       1h 30min WorkDuration (15min + 15min + 1h)
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

            final WorkDuration actual = sut.calculateWorkDuration(List.of(workEntry1, workEntry2, workEntry3, breakEntry));
            assertThat(actual.durationInMinutes()).isEqualTo(Duration.ofHours(1).plusMinutes(30));
        }

        @Test
        void workDurationForTimeEntriesOverlappingAndBreakOverlappingBoth() {
            /*
             *  ----------------------                    ----------------------
             * | [work] 08:00 - 09:00 |                  | [work] 10:00 - 11:00 |       1h 15min WorkDuration (15min + 1h)
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

            final WorkDuration actual = sut.calculateWorkDuration(List.of(workEntry1, workEntry2, workEntry3, breakEntry));
            assertThat(actual.durationInMinutes()).isEqualTo(Duration.ofHours(1).plusMinutes(15));
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

            final WorkDuration actual = sut.calculateWorkDuration(List.of(workEntry, breakEntry1, breakEntry2));
            assertThat(actual.durationInMinutes()).isEqualTo(Duration.ofHours(7).plusMinutes(30));
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
