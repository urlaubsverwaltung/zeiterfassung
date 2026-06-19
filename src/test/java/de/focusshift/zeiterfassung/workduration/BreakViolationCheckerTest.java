package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BreakViolationCheckerTest {

    private final BreakViolationChecker sut = new BreakViolationChecker();

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final UserIdComposite USER = new UserIdComposite(new UserId("user-1"), new UserLocalId(1L));
    private static final ZonedDateTime BASE = ZonedDateTime.of(2024, 1, 15, 8, 0, 0, 0, BERLIN);

    private static final BreakViolation DAILY = new BreakViolation(BreakViolationType.DAILY);
    private static final BreakViolation CONTINUITY = new BreakViolation(BreakViolationType.CONTINUITY);

    private static TimeEntry workEntry(ZonedDateTime start, ZonedDateTime end) {
        return new TimeEntry(new TimeEntryId(1L), USER, "", start, end, false);
    }

    private static TimeEntry breakEntry(ZonedDateTime start, ZonedDateTime end) {
        return new TimeEntry(new TimeEntryId(2L), USER, "", start, end, true);
    }

    // --- empty / trivial ---

    @Test
    void check_returnsEmptyList_whenNoEntries() {
        assertThat(sut.check(List.of())).isEmpty();
    }

    @Test
    void check_returnsEmptyList_whenWorkDurationIsExactly6Hours() {
        var entry = workEntry(BASE, BASE.plusHours(6));
        assertThat(sut.check(List.of(entry))).isEmpty();
    }

    // --- daily rule (§4 Satz 1) ---

    @Test
    void check_returnsDailyViolation_whenGrossTimeExceeds6HoursAndNoBreakRecorded() {
        var work = workEntry(BASE, BASE.plusHours(6).plusMinutes(1));
        assertThat(sut.check(List.of(work))).contains(DAILY);
    }

    @Test
    void check_returnsDailyViolation_whenGrossTimeExceeds6HoursAndBreakTooShort() {
        var work = workEntry(BASE, BASE.plusHours(6));
        var pause = breakEntry(BASE.plusHours(6), BASE.plusHours(6).plusMinutes(29));
        // gross = 6h29m > 6h → need 30min, have 29min
        assertThat(sut.check(List.of(work, pause))).contains(DAILY);
    }

    @Test
    void check_returnsNoDailyViolation_whenGrossTimeExceeds6HoursAndBreakMeetsMinimum() {
        var work = workEntry(BASE, BASE.plusHours(6));
        var pause = breakEntry(BASE.plusHours(6), BASE.plusHours(6).plusMinutes(30));
        // gross = 6h30m > 6h → need 30min, have 30min
        assertThat(sut.check(List.of(work, pause))).doesNotContain(DAILY);
    }

    @Test
    void check_returnsDailyViolation_whenGrossTimeExceeds9HoursAndBreakTooShort() {
        var work = workEntry(BASE, BASE.plusHours(9));
        var pause = breakEntry(BASE.plusHours(9), BASE.plusHours(9).plusMinutes(44));
        // gross = 9h44m > 9h → need 45min, have 44min
        assertThat(sut.check(List.of(work, pause))).contains(DAILY);
    }

    @Test
    void check_returnsNoDailyViolation_whenGrossTimeExceeds9HoursAndBreakMeetsMinimum() {
        var work = workEntry(BASE, BASE.plusHours(9));
        var pause = breakEntry(BASE.plusHours(9), BASE.plusHours(9).plusMinutes(45));
        // gross = 9h45m > 9h → need 45min, have 45min
        assertThat(sut.check(List.of(work, pause))).doesNotContain(DAILY);
    }

    // --- continuity rule (§4 Satz 3) ---

    @Test
    void check_returnsContinuityViolation_whenSingleWorkBlockExceeds6Hours() {
        var work = workEntry(BASE, BASE.plusHours(6).plusMinutes(1));
        assertThat(sut.check(List.of(work))).contains(CONTINUITY);
    }

    @Test
    void check_returnsNoContinuityViolation_whenWorkBlockIsExactly6Hours() {
        var work = workEntry(BASE, BASE.plusHours(6));
        assertThat(sut.check(List.of(work))).doesNotContain(CONTINUITY);
    }

    @Test
    void check_returnsContinuityViolation_whenTwoWorkEntriesExceed6HoursWithoutQualifyingBreak() {
        // two entries, no break between them → one continuous block of 6h2m
        var work1 = workEntry(BASE, BASE.plusHours(3).plusMinutes(1));
        var work2 = workEntry(BASE.plusHours(3).plusMinutes(1), BASE.plusHours(6).plusMinutes(2));
        assertThat(sut.check(List.of(work1, work2))).contains(CONTINUITY);
    }

    @Test
    void check_returnsNoContinuityViolation_whenQualifyingBreakOf15MinResetsBlock() {
        var work1 = workEntry(BASE, BASE.plusHours(4));
        var pause = breakEntry(BASE.plusHours(4), BASE.plusHours(4).plusMinutes(15));
        var work2 = workEntry(BASE.plusHours(4).plusMinutes(15), BASE.plusHours(7).plusMinutes(15));
        // block1=4h, break=15min resets, block2=3h → no violation
        assertThat(sut.check(List.of(work1, pause, work2))).doesNotContain(CONTINUITY);
    }

    @Test
    void check_returnsContinuityViolation_whenBreakIsLessThan15MinAndDoesNotResetBlock() {
        var work1 = workEntry(BASE, BASE.plusHours(4));
        var shortPause = breakEntry(BASE.plusHours(4), BASE.plusHours(4).plusMinutes(14));
        var work2 = workEntry(BASE.plusHours(4).plusMinutes(14), BASE.plusHours(7));
        // block1=4h, break=14min does NOT reset, block continues: 4h + 3h = 7h > 6h → violation
        assertThat(sut.check(List.of(work1, shortPause, work2))).contains(CONTINUITY);
    }

    @Test
    void check_returnsContinuityViolation_whenGapBetweenEntriesIsNotAnInterruption() {
        // gap between entries (no isBreak entry) does not reset the block
        var work1 = workEntry(BASE, BASE.plusHours(3).plusMinutes(1));
        // gap: BASE+3h01m to BASE+5h (no entry)
        var work2 = workEntry(BASE.plusHours(5), BASE.plusHours(8));
        // block = 3h01m + 3h = 6h01m > 6h → violation
        assertThat(sut.check(List.of(work1, work2))).contains(CONTINUITY);
    }

    @Test
    void check_returnsBothViolations_whenDailyAndContinuityRulesAreBothViolated() {
        var work = workEntry(BASE, BASE.plusHours(6).plusMinutes(1));
        // 6h01m work, no break → daily (need 30min, have 0) and continuity (>6h)
        assertThat(sut.check(List.of(work))).containsExactlyInAnyOrder(DAILY, CONTINUITY);
    }
}