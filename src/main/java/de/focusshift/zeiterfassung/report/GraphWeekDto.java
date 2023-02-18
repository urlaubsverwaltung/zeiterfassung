package de.focusshift.zeiterfassung.report;

import java.util.List;

record GraphWeekDto(String yearMonthWeek, List<GraphDayDto> dayReports, Double maxHoursWorked,
                    Double averageHoursWorked) {

    public Double graphLegendMaxHour() {
        final Double maxShould = dayReports.stream().map(GraphDayDto::hoursWorkedShould).max(Double::compareTo).orElse(8.0);
        final double max = Math.max(maxHoursWorked(), maxShould);
        return max % 2 == 0 ? max + 2 : max + 1;
    }
}
