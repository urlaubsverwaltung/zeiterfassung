package de.focusshift.zeiterfassung.integration.workingtime;

import java.time.LocalDate;
import java.util.UUID;

/**
 * RabbitMQ event published when a working time has been deleted.
 *
 * @param id event identifier
 * @param tenantId tenant identifier
 * @param username user identifier
 * @param validFrom start date of the deleted working time's validity
 */
record WorkingTimeDeletedRabbitEvent(
    UUID id,
    String tenantId,
    String username,
    LocalDate validFrom
) {
}
