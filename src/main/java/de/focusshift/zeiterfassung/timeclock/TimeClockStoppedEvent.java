package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.ZonedDateTime;

/**
 * Event dispatched after a {@link TimeClock} has been stopped.
 *
 * @param userId the user who stopped the time clock
 * @param startedAt when the time clock was started
 * @param stoppedAt when the time clock was stopped
 * @param comment comment of the time clock
 * @param isBreak whether this time clock is a break
 */
public record TimeClockStoppedEvent(UserId userId, ZonedDateTime startedAt, ZonedDateTime stoppedAt, String comment, boolean isBreak) {
}
