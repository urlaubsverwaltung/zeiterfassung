package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.workingtime.ZeitDuration;

import java.time.Duration;

public record OvertimeHours(Duration duration) implements ZeitDuration {

    @Override
    public Duration duration() {
        return duration;
    }
}
