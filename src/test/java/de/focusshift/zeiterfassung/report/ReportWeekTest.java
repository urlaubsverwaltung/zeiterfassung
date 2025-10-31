package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.workduration.WorkDuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportWeekTest {

    @Test
    void ensureAverageDayWorkDurationIsEmptyWhenNoReportDays() {
        final ReportWeek sut = new ReportWeek(LocalDate.of(2023, 2, 13), List.of());
        assertThat(sut.averageDayWorkDuration()).isEqualTo(WorkDuration.ZERO);
    }

    @Test
    void ensureAverageDayWorkDuration() {

        final LocalDate date = LocalDate.of(2023, 2, 13);

        final ReportDay day1 = dayWithWorkDuration(new WorkDuration(Duration.ofHours(1)));
        final ReportDay day2 = dayWithWorkDuration(new WorkDuration(Duration.ofHours(2)));
        final ReportDay day3 = dayWithWorkDuration(new WorkDuration(Duration.ofHours(3)));
        final ReportDay day4 = dayWithWorkDuration(new WorkDuration(Duration.ofHours(4)));
        final ReportDay day5 = dayWithWorkDuration(new WorkDuration(Duration.ofHours(5)));
        final ReportDay day6 = dayWithWorkDuration(new WorkDuration(Duration.ofHours(6)));
        final ReportDay day7 = dayWithWorkDuration(new WorkDuration(Duration.ofHours(7)));

        final ReportWeek sut = new ReportWeek(date, List.of(day1, day2, day3, day4, day4, day5, day6, day7));
        assertThat(sut.averageDayWorkDuration()).isEqualTo(new WorkDuration(Duration.ofHours(4)));
    }

    @ParameterizedTest
    @CsvSource({
        "2021-12-27,52",
        "2022-01-03,1",
        "2022-09-26,39",
        "2022-12-26,52",
        "2023-01-02,1",
        "2024-12-23,52",
        "2024-12-30,1",
        "2025-01-06,2",
        "2025-12-22,52",
        "2025-12-29,1",
        "2026-01-05,2",
        "2026-12-28,53",
        "2027-01-04,1",
        "2027-12-27,52",
        "2028-01-03,1",
    })
    void ensureCalendarWeek(String date, int week) {
        final ReportWeek timeEntryWeek = new ReportWeek(LocalDate.parse(date), List.of());
        assertThat(timeEntryWeek.calenderWeek()).isEqualTo(week);
    }

    private ReportDay dayWithWorkDuration(WorkDuration workDuration) {

        final ReportDay day = mock(ReportDay.class);
        when(day.workDuration()).thenReturn(workDuration);

        return day;
    }
}
