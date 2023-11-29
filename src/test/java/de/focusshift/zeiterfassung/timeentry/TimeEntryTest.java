package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimeEntryTest {
    private static final ZoneId ZONE_ID_BERLIN = ZoneId.of("Europe/Berlin");

    @Test
    void ensureWorkDurationIsZeroIfBreak() {

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), anyUserIdComposite(), "hard work", from, to, true);

        assertThat(timeEntry.workDuration().duration()).isEqualTo(Duration.ZERO);
    }

    @Test
    void ensureWorkDurationIsCorrectItNotABreak() {

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), anyUserIdComposite(), "hard work", from, to, false);

        assertThat(timeEntry.workDuration().duration()).isEqualTo(Duration.ofHours(1));
    }

    private UserIdComposite anyUserIdComposite() {
        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        return new UserIdComposite(userId, userLocalId);
    }

    private static ZonedDateTime dateTime(int year, int month, int dayOfMonth, int hour, int minute) {
        return ZonedDateTime.of(LocalDateTime.of(year, month, dayOfMonth, hour, minute), ZONE_ID_BERLIN);
    }
}
