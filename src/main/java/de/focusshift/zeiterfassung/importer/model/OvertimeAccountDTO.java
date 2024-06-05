package de.focusshift.zeiterfassung.importer.model;

import java.time.Duration;

public record OvertimeAccountDTO(boolean allowed, Duration maxAllowedOvertime) {
}

