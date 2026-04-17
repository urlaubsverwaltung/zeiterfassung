package de.focusshift.zeiterfassung.timeclock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

final class TimeClockMapper {

    private TimeClockMapper() {
        //
    }

    static TimeClockDto timeClockToTimeClockDto(TimeClock timeClock, Clock clock) {

        final Instant startedAt = timeClock.startedAt().toInstant();

        final Duration duration = Duration.between(startedAt, Instant.now(clock));
        final long hours = duration.toHours();
        final int minutes = duration.toMinutesPart();
        final int seconds = duration.toSecondsPart();
        final String durationString = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        final ZoneId zoneId = timeClock.startedAt().getZone();

        final TimeClockDto timeClockDto = new TimeClockDto();
        timeClockDto.setStartedAt(startedAt);

        timeClockDto.setDate(LocalDate.ofInstant(startedAt, zoneId));
        timeClockDto.setTime(LocalTime.ofInstant(startedAt, zoneId));
        timeClockDto.setZoneId(timeClock.startedAt().getZone());
        timeClockDto.setComment(timeClock.comment());
        timeClockDto.setBreak(timeClock.isBreak());
        timeClockDto.setDuration(durationString);

        return timeClockDto;
    }
}
