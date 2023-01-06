package de.focusshift.zeiterfassung.report;

import java.time.Duration;
import java.util.Date;

record DetailDayEntryDto(String username, String comment, Date start, Date end) {

    public String getDuration() {
        final Duration duration = Duration.ofMillis(end.getTime() - start.getTime());
        return "%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart());
    }
}
