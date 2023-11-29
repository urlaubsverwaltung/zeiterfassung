package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReportDayTest {

    private static final ZoneId ZONE_ID_BERLIN = ZoneId.of("Europe/Berlin");

    @Test
    void ensureToRemoveBreaks() {

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        final ReportDayEntry reportDayEntry = new ReportDayEntry(batman, "hard work", from, to, true);

        final ReportDay reportDay = new ReportDay(LocalDate.of(2021, 1, 4), Map.of(batmanIdComposite, PlannedWorkingHours.EIGHT), Map.of(batmanIdComposite, List.of(reportDayEntry)), Map.of());

        assertThat(reportDay.workDuration().duration()).isEqualTo(Duration.ZERO);
    }

    private static ZonedDateTime dateTime(int year, int month, int dayOfMonth, int hour, int minute) {
        return ZonedDateTime.of(LocalDateTime.of(year, month, dayOfMonth, hour, minute), ZONE_ID_BERLIN);
    }
}
