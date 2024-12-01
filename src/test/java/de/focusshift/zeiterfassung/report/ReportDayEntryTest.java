package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReportDayEntryTest {

    private static final ZoneId ZONE_ID_BERLIN = ZoneId.of("Europe/Berlin");

    @Test
    void ensureWorkDurationIsZeroIfBreak() {

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        final ReportDayEntry reportDayEntry = new ReportDayEntry(null, anyUser(), "hard work", from, to, true);

        assertThat(reportDayEntry.workDuration().duration()).isEqualTo(Duration.ZERO);
    }

    @Test
    void ensureWorkDurationIsCorrectItNotABreak() {

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        final ReportDayEntry reportDayEntry = new ReportDayEntry(null, anyUser(), "hard work", from, to, false);

        assertThat(reportDayEntry.workDuration().duration()).isEqualTo(Duration.ofHours(1));
    }

    private static User anyUser() {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(1337L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        return new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
    }

    private static ZonedDateTime dateTime(int year, int month, int dayOfMonth, int hour, int minute) {
        return ZonedDateTime.of(LocalDateTime.of(year, month, dayOfMonth, hour, minute), ZONE_ID_BERLIN);
    }
}
