package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.ZeitDuration;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Describes one time entry of a user.
 *
 * <p>
 * Note that the {@link TimeEntry#workDuration()} is not a calculated one but the duration between start and end!
 * You may be interested in the calculated {@link WorkDuration} of a {@link TimeEntryDay}, actually.
 *
 * @param id id of this time entry
 * @param userIdComposite id composite of the corresponding user
 * @param comment comment of the time entry, never {@code null}
 * @param start start timestamp
 * @param end end timestamp, never {@code null} ({@linkplain de.focusshift.zeiterfassung.timeclock.TimeClock} is something with start but without end)
 * @param isBreak whether time entry is a break or not
 */
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

    /**
     * Returns the duration between start and end if this entry is not a break, {@link WorkDuration#ZERO} otherwise.
     *
     * <p>
     * Note that this {@link WorkDuration} must not be used for reports or if you have a day context for instance.
     * See {@link TimeEntryDay#workDuration()} for this!
     *
     * @return the work duration of this entry
     */
    public WorkDuration workDuration() {
        return isBreak ? WorkDuration.ZERO : new WorkDuration(duration());
    }
}
