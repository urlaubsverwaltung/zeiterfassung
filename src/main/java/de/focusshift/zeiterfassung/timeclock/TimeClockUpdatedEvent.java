package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.ZonedDateTime;

/**
 * Event dispatched after a running {@link TimeClock} has been updated.
 *
 * @param userId the user who updated the time clock
 * @param startedAt the (possibly updated) start time
 * @param comment the (possibly updated) comment
 * @param isBreak the (possibly updated) break flag
 */
public record TimeClockUpdatedEvent(UserId userId, ZonedDateTime startedAt, String comment, boolean isBreak) {
}
