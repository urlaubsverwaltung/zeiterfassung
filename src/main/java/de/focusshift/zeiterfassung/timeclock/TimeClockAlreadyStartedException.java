package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;

/**
 * Thrown when a {@linkplain TimeClock stopwatch} should be started while there is already
 * a running one for the same user. Only one running stopwatch is allowed per user (and tenant).
 */
class TimeClockAlreadyStartedException extends Exception {

    TimeClockAlreadyStartedException(UserId userId, Throwable cause) {
        super("time clock for userId=%s has been started already.".formatted(userId), cause);
    }
}
