package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.user.UserIdComposite;
import jakarta.annotation.Nullable;

import java.time.Duration;

public record OvertimeAccountUpdatedEvent(UserIdComposite userIdComposite, boolean isOvertimeAllowed,
                                          @Nullable Duration maxAllowedOvertime) {
}
