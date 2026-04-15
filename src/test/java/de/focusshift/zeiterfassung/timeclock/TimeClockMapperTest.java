package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class TimeClockMapperTest {

    @Test
    void ensureTimeClockDurationIsMappedForMoreThan24Hours() {

        final Clock clock = Clock.fixed(Instant.parse("2026-04-15T20:09:00.00Z"), UTC);

        final UserId userId = new UserId("uuid");
        final ZonedDateTime startedAt = ZonedDateTime.now(clock).minusDays(1).minusMinutes(1).minusSeconds(2);

        final TimeClock timeClock = new TimeClock(userId, startedAt);

        final TimeClockDto actual = TimeClockMapper.timeClockToTimeClockDto(timeClock, clock);
        assertThat(actual.getDuration()).isEqualTo("24:01:02");
    }
}
