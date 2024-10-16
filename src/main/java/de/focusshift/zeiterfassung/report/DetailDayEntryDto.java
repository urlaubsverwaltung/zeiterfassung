package de.focusshift.zeiterfassung.report;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.Duration;
import java.time.LocalTime;

import static org.springframework.format.annotation.DateTimeFormat.ISO.TIME;

record DetailDayEntryDto(
    String username,
    String comment,
    boolean isBreak,
    @DateTimeFormat(iso = TIME) LocalTime start,
    @DateTimeFormat(iso = TIME) LocalTime end
) {

    public Duration getDuration() {
        return Duration.between(start, end);
    }
}
