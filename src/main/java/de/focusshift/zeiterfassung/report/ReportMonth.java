package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.WorkDuration;

import java.time.Duration;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;

import static java.util.function.Predicate.not;

record ReportMonth(YearMonth yearMonth, List<ReportWeek> weeks) {

    public WorkDuration averageDayWorkDuration() {

        final double averageMinutes = weeks.stream()
            .map(ReportWeek::reportDays)
            .flatMap(Collection::stream)
            .map(ReportDay::workDuration)
            .filter(not(WorkDuration.ZERO::equals))
            .map(WorkDuration::minutes)
            .mapToLong(Duration::toMinutes)
            .average()
            .orElse(0.0);// o.O

        final Duration duration = Duration.ofMinutes(Math.round(averageMinutes));

        return new WorkDuration(duration);
    }
}
