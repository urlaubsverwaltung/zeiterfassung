package de.focusshift.zeiterfassung.integration.overtimeaccount;

import jakarta.annotation.Nullable;

import java.time.Duration;
import java.util.UUID;

/**
 * RabbitMQ event published when an overtime account has been updated.
 *
 * @param id event identifier
 * @param tenantId tenant identifier
 * @param username user identifier
 * @param isOvertimeAllowed whether overtime is allowed for this user
 * @param maxAllowedOvertime maximum allowed overtime, or {@code null} if unlimited
 */
record OvertimeAccountUpdatedRabbitEvent(UUID id, String tenantId, String username, boolean isOvertimeAllowed,
                                         @Nullable Duration maxAllowedOvertime) {
}
