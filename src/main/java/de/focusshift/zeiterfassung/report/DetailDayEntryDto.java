package de.focusshift.zeiterfassung.report;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.Duration;
import java.time.LocalTime;

import static org.springframework.format.annotation.DateTimeFormat.ISO.TIME;

record DetailDayEntryDto(
    Long timeEntryId,
    String username,
    Long userLocalId,
    String comment,
    boolean isBreak,
    @DateTimeFormat(iso = TIME) LocalTime start,
    @DateTimeFormat(iso = TIME) LocalTime end,
    String detailDialogUrl
) {

    public Duration getDuration() {
        Duration duration = Duration.between(start, end);
        // this is the case if timeentry ends on next day
        if (end.isBefore(start)) {
            return duration.plus(Duration.ofDays(1));
        }
        return duration;
    }
}
