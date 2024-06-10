package de.focusshift.zeiterfassung.importer.model;


import java.time.Duration;

public record WorkDayDTO(String dayOfWeek, Duration duration) {
}
