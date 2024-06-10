package de.focusshift.zeiterfassung.importer.model;

import java.time.ZonedDateTime;
import java.util.Optional;

public record TimeClockDTO(ZonedDateTime startedAt, String comment, boolean isBreak,
                           Optional<ZonedDateTime> stoppedAt) {
}
