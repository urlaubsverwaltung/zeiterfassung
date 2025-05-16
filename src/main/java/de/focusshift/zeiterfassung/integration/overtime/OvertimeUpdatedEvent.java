package de.focusshift.zeiterfassung.integration.overtime;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Overtime updated event published on rabbitmq after the fact that overtime has changed for a date.
 *
 * <p>
 * Note that the duration value can be ZERO here. This is different to {@link OvertimeEvent}.
 *
 * @param id event identifier
 * @param tenantId tenant identifier
 * @param username user identifier
 * @param date date when the overtime happens
 * @param duration duration of the overtime
 */
record OvertimeUpdatedEvent(
    UUID id,
    String tenantId,
    String username,
    LocalDate date,
    Duration duration
) {
}
