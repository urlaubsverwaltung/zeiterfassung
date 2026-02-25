package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.ZonedDateTime;

/**
 * Event dispatched after a {@link TimeClock} has been started.
 *
 * @param userId the user who started the time clock
 * @param startedAt when the time clock was started
 * @param comment comment of the time clock
 * @param isBreak whether this time clock is a break
 */
public record TimeClockStartedEvent(UserId userId, ZonedDateTime startedAt, String comment, boolean isBreak) {
}
