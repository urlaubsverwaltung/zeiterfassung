package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.Duration;
import java.time.ZonedDateTime;

public record TimeEntry(
    TimeEntryId id,
    UserIdComposite userIdComposite,
    String comment,
    ZonedDateTime start,
    ZonedDateTime end,
    boolean isBreak
) {

    /**
     * Returns the exact duration between {@code start} and {@code end} date.
     * Consider using {@linkplain TimeEntry#durationInMinutes()} if you are interested in a value rounded to minutes.
     *
     * @return duration without context if it is a {@linkplain BreakDuration} or {@linkplain WorkDuration}
     */
    public Duration duration() {
        return Duration.between(start, end);
    }

    /**
     * Returns the duration between {@code start} and {@code end} rounded up to full minutes
     * (e.g. {@code "PT30S"} -> {@code "PT1M"}).
     *
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
