package de.focusshift.zeiterfassung.report;

record ReportSelectedUserDurationAggregationDto(Long userId, String delta, boolean deltaNegative, String worked, String should) {
}
