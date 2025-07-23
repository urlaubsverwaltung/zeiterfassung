package de.focusshift.zeiterfassung.integration.overtime;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Overtime event published on rabbitmq after the fact that a person has
 * worked more or less than the person should work at a given day.
 *
 *
 * @param id event identifier
 * @param tenantId tenant identifier
 * @param username user identifier
 * @param date date when the overtime happens
 * @param duration duration of the overtime
 */
record OvertimeRabbitEvent(
    UUID id,
    String tenantId,
    String username,
    LocalDate date,
    Duration duration
) {
}
