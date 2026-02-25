package de.focusshift.zeiterfassung.integration.timeentry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * RabbitMQ event published when a time entry has been updated.
 *
 * @param id event identifier
 * @param tenantId tenant identifier
 * @param username user identifier
 * @param date current date of the time entry
 * @param locked current locked state
 * @param workDuration current duration of work
 */
record TimeEntryUpdatedRabbitEvent(
    UUID id,
    String tenantId,
    String username,
    LocalDate date,
    boolean locked,
    Duration workDuration
) {
}
