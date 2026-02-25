package de.focusshift.zeiterfassung.integration.workingtime;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * RabbitMQ event published when a working time has been updated.
 *
 * @param id event identifier
 * @param tenantId tenant identifier
 * @param username user identifier
 * @param validFrom start date of this working time's validity
 * @param federalState federal state for public holiday calculation
 * @param worksOnPublicHoliday whether the user works on public holidays
 * @param monday planned working hours on monday
 * @param tuesday planned working hours on tuesday
 * @param wednesday planned working hours on wednesday
 * @param thursday planned working hours on thursday
 * @param friday planned working hours on friday
 * @param saturday planned working hours on saturday
 * @param sunday planned working hours on sunday
 */
record WorkingTimeUpdatedRabbitEvent(
    UUID id,
    String tenantId,
    String username,
    LocalDate validFrom,
    String federalState,
    Boolean worksOnPublicHoliday,
    Duration monday,
    Duration tuesday,
    Duration wednesday,
    Duration thursday,
    Duration friday,
    Duration saturday,
    Duration sunday
) {
}
