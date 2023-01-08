package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.ZonedDateTime;

/**
 * Information that should be updated for the currently running {@linkplain TimeClock}.
 *
 * @param userId id of the user to get the current {@linkplain TimeClock} for
 * @param startedAt new value of startedAt
 * @param comment new value of comment
 */
record TimeClockUpdate(UserId userId, ZonedDateTime startedAt, String comment) {
}
