package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.Duration;
import java.time.ZonedDateTime;

public record TimeEntry(
    TimeEntryId id,
    UserId userId,
    String comment,
    ZonedDateTime start,
    ZonedDateTime end,
    boolean isBreak
) {

    /**
     * @return duration without context if it is a {@linkplain BreakDuration} or {@linkplain WorkDuration}
     */
    public Duration duration() {
        return Duration.between(start, end);
    }

    /**
     * @return duration in minutes without context if it is a {@linkplain BreakDuration} or {@linkplain WorkDuration}
     */
    public Duration durationInMinutes() {
        return ZeitDuration.of(Duration.between(start, end)).durationInMinutes();
    }

    public BreakDuration breakDuration() {
        return new BreakDuration(isBreak ? duration() : Duration.ZERO);
    }

    public WorkDuration workDuration() {
        return new WorkDuration(isBreak ? Duration.ZERO : duration());
    }
}
