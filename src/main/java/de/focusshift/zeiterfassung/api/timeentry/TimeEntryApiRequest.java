package de.focusshift.zeiterfassung.api.timeentry;

import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record TimeEntryApiRequest(
    @NotNull ZonedDateTime start,
    @NotNull ZonedDateTime end,
    String comment,
    boolean isBreak
) {
}
