package de.focusshift.zeiterfassung.integration.timeclock;

import java.time.Instant;
import java.util.UUID;

/**
 * RabbitMQ event published when a time clock has been started.
 *
 * @param id event identifier
 * @param tenantId tenant identifier
 * @param username user identifier
 * @param startedAt when the time clock was started
 * @param comment comment of the time clock
 * @param isBreak whether this time clock is a break
 */
record TimeClockStartedRabbitEvent(
    UUID id,
    String tenantId,
    String username,
    Instant startedAt,
    String comment,
    boolean isBreak
) {
}
