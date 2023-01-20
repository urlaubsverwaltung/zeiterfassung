package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.Duration;
import java.time.ZonedDateTime;

public record TimeEntry(
    Long id,
    UserId userId,
    String comment,
    ZonedDateTime start,
    ZonedDateTime end,
    boolean isBreak
) {

    public WorkDuration workDuration() {
        return new WorkDuration(isBreak ? Duration.ZERO : Duration.between(start, end));
    }
}
