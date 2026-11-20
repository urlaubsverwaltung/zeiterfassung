package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BreakViolationCheckerTest {

    private final BreakViolationChecker sut = new BreakViolationChecker();

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final UserIdComposite USER = new UserIdComposite(new UserId("user-1"), new UserLocalId(1L));

    private static final BreakViolation DAILY = new BreakViolation(BreakViolationType.DAILY);
    private static final BreakViolation CONTINUITY = new BreakViolation(BreakViolationType.CONTINUITY);

    private static long id = 0;

    private static ZonedDateTime at(int hour, int minute) {
        return ZonedDateTime.of(2024, 1, 15, hour, minute, 0, 0, BERLIN);
    }

    private static TimeEntry work(ZonedDateTime start, ZonedDateTime end) {
        return new TimeEntry(new TimeEntryId(++id), USER, "", start, end, false);
    }

    private static TimeEntry pause(ZonedDateTime start, ZonedDateTime end) {
        return new TimeEntry(new TimeEntryId(++id), USER, "", start, end, true);
    }

    private static WorkDuration hours(int hours, int minutes) {
        return new WorkDuration(Duration.ofHours(hours).plusMinutes(minutes));
    }

    @Test
    void noViolationWithoutEntries() {
        assertThat(sut.check(WorkDuration.ZERO, List.of())).isEmpty();
    }

    @Test
    void noViolationForShortDay() {
        // EC-5
        assertThat(sut.check(hours(2, 0), List.of(work(at(8, 0), at(10, 0))))).isEmpty();
    }

    @Test
    void noViolationForExactlySixHours() {
        // EC-1
        assertThat(sut.check(hours(6, 0), List.of(work(at(8, 0), at(14, 0))))).isEmpty();
    }

    // --- daily rule (§4 Satz 1) ---

    @Test
    void dailyViolationWhenMoreThanSixHoursWithoutBreak() {
        assertThat(sut.check(hours(6, 1), List.of(work(at(8, 0), at(14, 1))))).contains(DAILY);
    }

    @Test
    void dailyViolationWhenBreakIsShorterThanMandatoryBreak() {
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(12, 0)),
            pause(at(12, 0), at(12, 20)),
            work(at(12, 20), at(16, 20))
        );
        assertThat(sut.check(hours(8, 0), entries)).contains(DAILY);
    }

    @Test
    void noDailyViolationWhenBreakMeetsMandatoryBreak() {
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(12, 0)),
            pause(at(12, 0), at(12, 30)),
            work(at(12, 30), at(16, 30))
        );
        assertThat(sut.check(hours(8, 0), entries)).isEmpty();
    }

    @Test
    void breakEntriesDoNotCountAsWorkDuration() {
        // 6h work + 29 min break. the work duration is 6h, no break is mandatory.
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(14, 0)),
            pause(at(14, 0), at(14, 29))
        );
        assertThat(sut.check(hours(6, 0), entries)).doesNotContain(DAILY);
    }

    @Test
    void dailyViolationWhenMoreThanNineHoursAndBreakShorterThan45Minutes() {
        final List<TimeEntry> entries = List.of(
            work(at(7, 0), at(12, 0)),
            pause(at(12, 0), at(12, 44)),
            work(at(12, 44), at(17, 45))
        );
        assertThat(sut.check(hours(10, 1), entries)).contains(DAILY);
    }

    @Test
    void noDailyViolationWhenMoreThanNineHoursAndBreakOf45Minutes() {
        final List<TimeEntry> entries = List.of(
            work(at(7, 0), at(12, 0)),
            pause(at(12, 0), at(12, 45)),
            work(at(12, 45), at(17, 45))
        );
        assertThat(sut.check(hours(10, 0), entries)).isEmpty();
    }

    @Test
    void gapWithoutBreakEntryIsNoBreakButInterruptsTheWork() {
        // EC-7: the gap does not count as break for the daily rule, but nobody works during the gap
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(12, 0)),
            work(at(13, 0), at(17, 0))
        );
        assertThat(sut.check(hours(8, 0), entries)).containsExactly(DAILY);
    }

    @Test
    void breaksShorterThan15MinutesDoNotCount() {
        // EC-8
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(14, 10)),
            pause(at(14, 10), at(14, 20)),
            work(at(14, 20), at(17, 0))
        );
        assertThat(sut.check(hours(8, 50), entries)).containsExactlyInAnyOrder(DAILY, CONTINUITY);
    }

    @Test
    void adjacentShortBreaksAreMergedBeforeCheckingTheMinimumDuration() {
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(12, 0)),
            pause(at(12, 0), at(12, 10)),
            pause(at(12, 10), at(12, 30)),
            work(at(12, 30), at(16, 30))
        );
        assertThat(sut.check(hours(8, 0), entries)).isEmpty();
    }

    @Test
    void severalBreaksOfAtLeast15MinutesAddUp() {
        // EC-9
        final List<TimeEntry> entries = List.of(
            work(at(6, 0), at(9, 0)),
            pause(at(9, 0), at(9, 15)),
            work(at(9, 15), at(12, 0)),
            pause(at(12, 0), at(12, 15)),
            work(at(12, 15), at(14, 30))
        );
        assertThat(sut.check(hours(8, 0), entries)).isEmpty();
    }

    @Test
    void breakWithinWorkEntryCounts() {
        // time clock: one work entry for the whole day, the break recorded within it
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(16, 30)),
            pause(at(12, 0), at(12, 30))
        );
        assertThat(sut.check(hours(8, 0), entries)).isEmpty();
    }

    // --- continuity rule (§4 Satz 3) ---

    @Test
    void continuityViolationWhenDailyRuleIsMet() {
        // EC-6
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(14, 30)),
            pause(at(14, 30), at(15, 0)),
            work(at(15, 0), at(16, 0))
        );
        assertThat(sut.check(hours(7, 30), entries)).containsExactly(CONTINUITY);
    }

    @Test
    void continuityCountsWorkEntriesWithGapShorterThan15MinutesAsOneBlock() {
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(11, 0)),
            work(at(11, 10), at(14, 11))
        );
        assertThat(sut.check(hours(6, 1), entries)).contains(CONTINUITY);
    }

    @Test
    void continuityDoesNotCountShortBreaksWithinWorkEntryAsWork() {
        // 6h10 work entry with 10 minutes break within it -> 6h of work
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(14, 10)),
            pause(at(11, 0), at(11, 10))
        );
        assertThat(sut.check(hours(6, 0), entries)).isEmpty();
    }

    @Test
    void breakAfterWorkDoesNotCount() {
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(15, 30)),
            pause(at(15, 30), at(16, 0))
        );
        assertThat(sut.check(hours(7, 30), entries)).containsExactlyInAnyOrder(DAILY, CONTINUITY);
    }

    @Test
    void breakBeforeWorkDoesNotCount() {
        final List<TimeEntry> entries = List.of(
            pause(at(7, 30), at(8, 0)),
            work(at(8, 0), at(12, 0)),
            work(at(12, 15), at(15, 30))
        );
        assertThat(sut.check(hours(7, 15), entries)).containsExactly(DAILY);
    }

    @Test
    void noContinuityViolationWhenQualifyingBreakSplitsBlocks() {
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(12, 0)),
            pause(at(12, 0), at(12, 15)),
            pause(at(14, 0), at(14, 15)),
            work(at(12, 15), at(16, 15))
        );
        assertThat(sut.check(hours(8, 0), entries)).isEmpty();
    }

    @Test
    void continuityViolationWhenBreakWithinWorkEntryIsTooLate() {
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(16, 30)),
            pause(at(14, 15), at(14, 45))
        );
        assertThat(sut.check(hours(8, 0), entries)).containsExactly(CONTINUITY);
    }
}
