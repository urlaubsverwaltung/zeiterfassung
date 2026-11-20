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

class MandatoryBreakDeductionCalculatorTest {

    private final MandatoryBreakDeductionCalculator sut = new MandatoryBreakDeductionCalculator();

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final UserIdComposite USER = new UserIdComposite(new UserId("user-1"), new UserLocalId(1L));

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
    void noDeductionUpToSixHours() {
        // EC-1
        assertThat(sut.calculateDeduction(hours(6, 0), List.of(work(at(8, 0), at(14, 0))))).isZero();
    }

    @Test
    void deducts30MinutesForMoreThanSixHoursWithoutBreak() {
        assertThat(sut.calculateDeduction(hours(6, 1), List.of(work(at(8, 0), at(14, 1)))))
            .isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void deducts30MinutesForExactlyNineHours() {
        // EC-4
        assertThat(sut.calculateDeduction(hours(9, 0), List.of(work(at(8, 0), at(17, 0)))))
            .isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void deducts45MinutesForMoreThanNineHoursWithoutBreak() {
        assertThat(sut.calculateDeduction(hours(9, 1), List.of(work(at(8, 0), at(17, 1)))))
            .isEqualTo(Duration.ofMinutes(45));
    }

    @Test
    void noDeductionWhenRecordedBreakExceedsMandatoryBreak() {
        // EC-2
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(12, 0)),
            pause(at(12, 0), at(12, 45)),
            work(at(12, 45), at(16, 45))
        );
        assertThat(sut.calculateDeduction(hours(8, 0), entries)).isZero();
    }

    @Test
    void deductsOnlyTheMissingPartOfTheMandatoryBreak() {
        // EC-3
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(12, 0)),
            pause(at(12, 0), at(12, 15)),
            work(at(12, 15), at(16, 15))
        );
        assertThat(sut.calculateDeduction(hours(8, 0), entries)).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void breaksShorterThan15MinutesAreNotCredited() {
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(10, 0)),
            pause(at(10, 0), at(10, 10)),
            work(at(10, 10), at(13, 0)),
            pause(at(13, 0), at(13, 10)),
            work(at(13, 10), at(16, 20))
        );
        assertThat(sut.calculateDeduction(hours(8, 0), entries)).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void breakWithinWorkEntryIsCredited() {
        // the overlapping break has already been subtracted from the work duration: 8h30 - 30min = 8h
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(16, 30)),
            pause(at(12, 0), at(12, 30))
        );
        assertThat(sut.calculateDeduction(hours(8, 0), entries)).isZero();
    }

    @Test
    void breakOutsideOfTheWorkIsNotCredited() {
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(15, 30)),
            pause(at(15, 30), at(16, 0))
        );
        assertThat(sut.calculateDeduction(hours(7, 30), entries)).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void onlyThePartOfABreakWithinTheWorkIsCredited() {
        // break 11:50 - 12:20, work starts 12:00 -> 20 minutes credited
        final List<TimeEntry> entries = List.of(
            pause(at(11, 50), at(12, 20)),
            work(at(12, 0), at(19, 0))
        );
        assertThat(sut.calculateDeduction(hours(6, 40), entries)).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void mandatoryBreakDependsOnTheGivenWorkDuration() {
        // 6h20 work entry with 20 minutes break within it.
        // with subtracted overlapping breaks the work duration is 6h -> no mandatory break.
        final List<TimeEntry> entries = List.of(
            work(at(8, 0), at(14, 20)),
            pause(at(10, 0), at(10, 20))
        );
        assertThat(sut.calculateDeduction(hours(6, 0), entries)).isZero();
        assertThat(sut.calculateDeduction(hours(6, 20), entries)).isEqualTo(Duration.ofMinutes(10));
    }
}
