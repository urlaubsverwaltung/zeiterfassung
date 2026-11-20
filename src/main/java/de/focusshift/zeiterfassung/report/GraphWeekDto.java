package de.focusshift.zeiterfassung.report;

import java.util.List;

record GraphWeekDto(
    int calendarWeek,
    String dateRangeString,
    List<GraphDayDto> dayReports,
    Double maxHoursWorked,
    String workedWorkingHours,
    String shouldWorkingHours,
    String hoursDelta,
    boolean hoursDeltaNegative,
    double hoursWorkedRatio
) {

    // keeps the number of legend lines readable when hours of many persons are summed up
    private static final int[] LEGEND_STEPS = {2, 4, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000, 2500, 5000};
    private static final int LEGEND_MAX_LINES = 10;

    public Double graphLegendMaxHour() {

        final Double maxShould = dayReports.stream()
            .map(GraphDayDto::hoursWorkedShould)
            .max(Double::compareTo)
            .orElse(8.0);

        final double max = Math.max(maxHoursWorked(), maxShould);
        return graphLegendMax(max);
    }

    public int graphLegendStepHour() {
        return graphLegendStep(graphLegendMaxHour());
    }

    static int graphLegendStep(double max) {
        for (int step : LEGEND_STEPS) {
            if (max / step <= LEGEND_MAX_LINES) {
                return step;
            }
        }
        return LEGEND_STEPS[LEGEND_STEPS.length - 1];
    }

    static double graphLegendMax(double max) {
        final double withHeadroom = max + 1;
        final int step = graphLegendStep(withHeadroom);
        return Math.ceil(withHeadroom / step) * step;
    }
}
