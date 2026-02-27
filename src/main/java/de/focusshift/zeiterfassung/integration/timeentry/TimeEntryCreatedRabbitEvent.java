package de.focusshift.zeiterfassung.integration.timeentry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * RabbitMQ event published when a time entry has been created.
 *
 * @param id event identifier
 * @param tenantId tenant identifier
 * @param username user identifier
 * @param date date of the time entry
 * @param locked whether the time entry is locked
 * @param workDuration duration of work
 */
record TimeEntryCreatedRabbitEvent(
    UUID id,
    String tenantId,
    String username,
    LocalDate date,
    boolean locked,
    Duration workDuration
) {
}
