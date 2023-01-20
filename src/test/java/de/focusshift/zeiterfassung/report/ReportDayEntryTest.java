package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReportDayEntryTest {

    private static final ZoneId ZONE_ID_BERLIN = ZoneId.of("Europe/Berlin");

    @Test
    void ensureWorkDurationIsZeroIfBreak() {

        final User batman = new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress(""));

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        final ReportDayEntry reportDayEntry = new ReportDayEntry(batman, "hard work", from, to, true);

        assertThat(reportDayEntry.workDuration().duration()).isEqualTo(Duration.ZERO);
    }

    @Test
    void ensureWorkDurationIsCorrectItNotABreak() {

        final User batman = new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress(""));

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        final ReportDayEntry reportDayEntry = new ReportDayEntry(batman, "hard work", from, to, false);

        assertThat(reportDayEntry.workDuration().duration()).isEqualTo(Duration.ofHours(1));
    }

    private static ZonedDateTime dateTime(int year, int month, int dayOfMonth, int hour, int minute) {
        return ZonedDateTime.of(LocalDateTime.of(year, month, dayOfMonth, hour, minute), ZONE_ID_BERLIN);
    }
}
