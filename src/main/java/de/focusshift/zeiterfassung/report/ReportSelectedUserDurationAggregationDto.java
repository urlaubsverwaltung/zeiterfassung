package de.focusshift.zeiterfassung.report;

record ReportSelectedUserDurationAggregationDto(Long userId, String fullName, String delta, boolean deltaNegative, String worked, String should) {
}
