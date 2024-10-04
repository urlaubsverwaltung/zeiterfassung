package de.focusshift.zeiterfassung.report;

import java.util.List;

record GraphMonthDto(String yearMonth, List<GraphWeekDto> weekReports, Double maxHoursWorked) {

    public Double graphLegendMaxHour() {

        final Double maxShould = weekReports.stream()
            .map(GraphWeekDto::graphLegendMaxHour)
            .max(Double::compareTo)
            .orElse(8.0);

        final double max = Math.max(maxHoursWorked(), maxShould);
        return max % 2 == 0 ? max + 2 : max + 1;
    }
}
