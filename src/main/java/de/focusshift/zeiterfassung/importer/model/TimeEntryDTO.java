package de.focusshift.zeiterfassung.importer.model;

import java.time.ZonedDateTime;

public record TimeEntryDTO(String comment, ZonedDateTime start, ZonedDateTime end, boolean isBreak, boolean isFreezed) {
}

