package de.focusshift.zeiterfassung.report;

import java.util.List;

record ReportOvertimeDto(String personName, List<Double> overtimes) {

    public Double overtimeSum() {
        return overtimes.stream().reduce(0d, Double::sum);
    }
}
