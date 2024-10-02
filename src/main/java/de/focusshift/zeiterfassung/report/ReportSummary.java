package de.focusshift.zeiterfassung.report;

import java.time.Duration;

record ReportSummary(Duration plannedWorkingHours, Duration hoursWorked, Duration hoursDelta) {}
