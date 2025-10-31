package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.workduration.WorkDuration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportMonthTest {

    @Test
    void ensureAverageDayWorkDuration() {

        final ReportWeek week1 = weekWithAvgWorkDuration(new WorkDuration(Duration.ofHours(1)));
        final ReportWeek week2 = weekWithAvgWorkDuration(new WorkDuration(Duration.ofHours(2)));
        final ReportWeek week3 = weekWithAvgWorkDuration(new WorkDuration(Duration.ofHours(3)));
        final ReportWeek week4 = weekWithAvgWorkDuration(new WorkDuration(Duration.ofHours(4)));
        final ReportWeek week5 = weekWithAvgWorkDuration(new WorkDuration(Duration.ofHours(5)));

        final ReportMonth sut = new ReportMonth(YearMonth.of(2023, 2), List.of(week1, week2, week3, week4, week5));

        final WorkDuration actual = sut.averageDayWorkDuration();
        assertThat(actual).isEqualTo(new WorkDuration(Duration.ofHours(3)));
    }

    private ReportWeek weekWithAvgWorkDuration(WorkDuration average) {

        final ReportWeek week = mock(ReportWeek.class);
        when(week.averageDayWorkDuration()).thenReturn(average);

        return week;
    }
}
