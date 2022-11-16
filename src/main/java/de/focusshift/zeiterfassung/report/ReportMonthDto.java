package de.focusshift.zeiterfassung.report;

import java.util.List;

record ReportMonthDto(String yearMonth, List<ReportWeekDto> weekReports, Double maxHoursWorked,
                      Double averageHoursWorked) {

    public Double graphLegendMaxHour() {
        final double max = Math.max(maxHoursWorked(), 8);
        return max % 2 == 0 ? max + 2 : max + 1;
    }
}
