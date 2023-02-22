package de.focusshift.zeiterfassung.report;

record GraphDayDto(boolean differentMonth,
                   String dayOfWeek,
                   String dayOfWeekFull,
                   String date,
                   Double hoursWorked,
                   Double hoursWorkedShould) {
}
