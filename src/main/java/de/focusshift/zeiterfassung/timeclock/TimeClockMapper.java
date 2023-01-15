package de.focusshift.zeiterfassung.timeclock;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

final class TimeClockMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private TimeClockMapper() {
        //
    }

    static TimeClockDto timeClockToTimeClockDto(TimeClock timeClock) {

        final Instant startedAt = timeClock.startedAt().toInstant();

        final Duration duration = Duration.between(startedAt, Instant.now());
        final int hours = duration.toHoursPart();
        final int minutes = duration.toMinutesPart();
        final int seconds = duration.toSecondsPart();
        final String durationString = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        final ZoneId zoneId = timeClock.startedAt().getZone();

        final TimeClockDto timeClockDto = new TimeClockDto();
        timeClockDto.setStartedAt(startedAt);

        timeClockDto.setDate(ZonedDateTime.ofInstant(startedAt, zoneId).format(DATE_FORMATTER));
        timeClockDto.setTime(ZonedDateTime.ofInstant(startedAt, zoneId).format(TIME_FORMATTER));
        timeClockDto.setZoneId(timeClock.startedAt().getZone());
        timeClockDto.setComment(timeClock.comment());
        timeClockDto.setDuration(durationString);

        return timeClockDto;
    }
}
