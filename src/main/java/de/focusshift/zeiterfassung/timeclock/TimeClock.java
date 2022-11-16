package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.ZonedDateTime;
import java.util.Optional;

record TimeClock(Long id, UserId userId, ZonedDateTime startedAt, Optional<ZonedDateTime> stoppedAt) {

    TimeClock(UserId userId, ZonedDateTime startedAt) {
        this(null, userId, startedAt, Optional.empty());
    }
}
