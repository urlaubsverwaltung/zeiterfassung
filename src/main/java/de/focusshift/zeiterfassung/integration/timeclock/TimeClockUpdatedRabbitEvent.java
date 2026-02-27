package de.focusshift.zeiterfassung.integration.timeclock;

import java.time.Instant;
import java.util.UUID;

/**
 * RabbitMQ event published when a running time clock has been updated.
 *
 * @param id event identifier
 * @param tenantId tenant identifier
 * @param username user identifier
 * @param startedAt the (possibly updated) start time
 * @param comment the (possibly updated) comment
 * @param isBreak the (possibly updated) break flag
 */
record TimeClockUpdatedRabbitEvent(
    UUID id,
    String tenantId,
    String username,
    Instant startedAt,
    String comment,
    boolean isBreak
) {
}
