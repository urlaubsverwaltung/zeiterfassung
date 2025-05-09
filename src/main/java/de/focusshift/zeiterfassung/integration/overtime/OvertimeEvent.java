package de.focusshift.zeiterfassung.integration.overtime;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Overtime event published on rabbitmq.
 *
 * @param id event identifier
 * @param tenantId tenant id
 * @param username user identifier
 * @param date date
 * @param duration duration
 */
record OvertimeEvent(
    UUID id,
    String tenantId,
    String username,
    LocalDate date,
    Duration duration
) {
}
