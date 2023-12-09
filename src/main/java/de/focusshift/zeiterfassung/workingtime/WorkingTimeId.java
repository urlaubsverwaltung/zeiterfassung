package de.focusshift.zeiterfassung.workingtime;

import java.util.UUID;

import static org.springframework.util.Assert.notNull;

public record WorkingTimeId(UUID uuid) {

    public WorkingTimeId {
        notNull(uuid, "expected uuid not to be null");
    }

    public static WorkingTimeId fromString(String value) {
        notNull(value, "expected value not to be null");
        return new WorkingTimeId(UUID.fromString(value));
    }

    public String value() {
        return uuid.toString();
    }
}
