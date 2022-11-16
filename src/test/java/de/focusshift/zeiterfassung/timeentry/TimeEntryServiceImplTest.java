package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceImplTest {

    private static final ZoneId ZONE_ID_UTC = ZoneId.of("UTC");

    private TimeEntryServiceImpl sut;

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private UserDateService userDateService;

    @Mock
    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, userDateService);
    }

    @Test
    void ensureGetEntries() {

        final LocalDate periodFrom = LocalDate.of(2022, 1, 3);
        final LocalDate periodToExclusive = LocalDate.of(2022, 1, 10);

        final LocalDateTime entryStart = LocalDateTime.of(periodFrom, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(17, 0, 0));
        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity(1L, "batman", "hard work", entryStart.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"), entryEnd.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"), Instant.now());

        final Instant periodStartInstant = periodFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
        final Instant periodEndInstant = periodToExclusive.atStartOfDay(ZoneOffset.UTC).toInstant();
        when(timeEntryRepository.findAllByOwnerAndTouchingPeriod("batman", periodStartInstant, periodEndInstant))
            .thenReturn(List.of(timeEntryEntity));

        final List<TimeEntry> actualEntries = sut.getEntries(periodFrom, periodToExclusive, new UserId("batman"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(entryStart, ZONE_ID_UTC);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(entryEnd, ZONE_ID_UTC);

        assertThat(actualEntries).containsExactly(
            new TimeEntry(1L, new UserId("batman"), "hard work", expectedStart, expectedEnd)
        );
    }

    @Test
    void ensureGetEntryWeekPageWithFirstDayOfMonth() {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");

        when(userDateService.firstDayOfWeek(Year.of(2022), 1)).thenReturn(LocalDate.of(2022, 1, 3));

        final ZonedDateTime timeEntryStart = ZonedDateTime.of(2022, 1, 4, 9, 0, 0, 0, zoneIdBerlin);
        final ZonedDateTime timeEntryEnd = ZonedDateTime.of(2022, 1, 4, 17, 0, 0, 0, zoneIdBerlin);

        final Instant timeEntryStartInstant = timeEntryStart.toInstant();
        final Instant timeEntryEndInstant = timeEntryEnd.toInstant();
        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity("tenantId", 1L, "batman", "hack the planet!", timeEntryStartInstant, zoneIdBerlin, timeEntryEndInstant, zoneIdBerlin, timeEntryStartInstant);

        final ZonedDateTime fromDateTime = LocalDate.of(2022, 1, 3).atStartOfDay(ZoneId.systemDefault());
        final Instant from = Instant.from(fromDateTime);
        final Instant to = Instant.from(fromDateTime.plusWeeks(1));

        when(timeEntryRepository.findAllByOwnerAndTouchingPeriod("batman", from , to)).thenReturn(List.of(timeEntryEntity));
        when(timeEntryRepository.countAllByOwner("batman")).thenReturn(3L);

        final TimeEntryWeekPage actual = sut.getEntryWeekPage(new UserId("batman"), 2022, 1);

        assertThat(actual).isEqualTo(
            new TimeEntryWeekPage(
                new TimeEntryWeek(
                    LocalDate.of(2022, 1, 3),
                    List.of(
                        new TimeEntry(1L, new UserId("batman"), "hack the planet!", timeEntryStart, timeEntryEnd)
                    )
                ),
                3
            )
        );
    }
}
