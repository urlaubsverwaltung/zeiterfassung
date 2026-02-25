package de.focusshift.zeiterfassung.integration.timeentry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * RabbitMQ event published when a time entry has been deleted.
 *
 * @param id event identifier
 * @param tenantId tenant identifier
 * @param username user identifier
 * @param date date of the deleted time entry
 * @param locked whether the deleted time entry was locked
 * @param workDuration duration of work of the deleted time entry
 */
record TimeEntryDeletedRabbitEvent(
    UUID id,
    String tenantId,
    String username,
    LocalDate date,
    boolean locked,
    Duration workDuration
) {
}
