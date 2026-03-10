package de.focusshift.zeiterfassung.api.timeentry;

import java.time.Duration;
import java.time.ZonedDateTime;

public record TimeEntryApiResponse(
    Long id,
    ZonedDateTime start,
    ZonedDateTime end,
    Duration duration,
    String comment,
    boolean isBreak
) {
}
