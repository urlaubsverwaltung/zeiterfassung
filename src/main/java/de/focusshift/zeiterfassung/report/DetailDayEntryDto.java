package de.focusshift.zeiterfassung.report;

import java.time.Duration;
import java.util.Date;

record DetailDayEntryDto(String username, String comment, Date start, Date end) {

    public Duration getDuration() {
        return Duration.ofMillis(end.getTime() - start.getTime());
    }
}
