package de.focusshift.zeiterfassung.report;

import java.util.List;

record GraphMonthDto(
    String yearMonth,
    List<GraphWeekDto> weekReports,
    Double maxHoursWorked,
    String workedWorkingHours,
    String shouldWorkingHours,
    String hoursDelta,
    boolean hoursDeltaNegative,
    double hoursWorkedRatio
) {

    public Double graphLegendMaxHour() {

        final Double maxShould = weekReports.stream()
            .map(GraphWeekDto::graphLegendMaxHour)
            .max(Double::compareTo)
            .orElse(8.0);

        final double max = Math.max(maxHoursWorked(), maxShould);
        return GraphWeekDto.graphLegendMax(max);
    }

    public int graphLegendStepHour() {
        return GraphWeekDto.graphLegendStep(graphLegendMaxHour());
    }
}
