package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;

public class TimeClockNotStartedException extends Exception {

    TimeClockNotStartedException(UserId userId) {
        super("time clock for userId=%s is not running.".formatted(userId));
    }
}
