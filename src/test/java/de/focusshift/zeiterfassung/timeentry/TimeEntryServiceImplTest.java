package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, userDateService, clock);
    }

    @Test
    void ensureSaveTimeEntry() {

        final Instant now = Instant.now();
        final Clock fixedClock = Clock.fixed(now, UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, userDateService, fixedClock);

        final LocalDateTime entryStart = LocalDateTime.of(2023, 1, 1, 10, 0,0);
        final LocalDateTime entryEnd = LocalDateTime.of(2023, 1, 1, 12, 0,0);

        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final TimeEntry timeEntry = new TimeEntry(1L, new UserId("batman"), "hard work", ZonedDateTime.of(entryStart, ZONE_ID_UTC), ZonedDateTime.of(entryEnd, ZONE_ID_UTC), false);

        final TimeEntry actual = sut.saveTimeEntry(timeEntry);

        assertThat(actual).isEqualTo(new TimeEntry(1L, new UserId("batman"), "hard work", ZonedDateTime.of(entryStart, ZONE_ID_UTC), ZonedDateTime.of(entryEnd, ZONE_ID_UTC), false));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        assertThat(captor.getValue()).satisfies(entity -> {
            assertThat(entity.getOwner()).isEqualTo("batman");
            assertThat(entity.getComment()).isEqualTo("hard work");
            assertThat(entity.getStart()).isEqualTo(entryStart.toInstant(UTC));
            assertThat(entity.getStartZoneId()).isEqualTo(ZONE_ID_UTC.getId());
            assertThat(entity.getEnd()).isEqualTo(entryEnd.toInstant(UTC));
            assertThat(entity.getEndZoneId()).isEqualTo(ZONE_ID_UTC.getId());
            assertThat(entity.getUpdatedAt()).isEqualTo(now);
            assertThat(entity.isBreak()).isFalse();
        });
    }

    @Test
    void ensureGetEntriesForAllUsers() {

        final Instant now = Instant.now();
        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2023, 2, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(toExclusive, LocalTime.of(12, 0, 0));
        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity(1L, "batman", "hard work", entryStart.toInstant(UTC), ZONE_ID_UTC, entryEnd.toInstant(UTC), ZONE_ID_UTC, now, false);

        final LocalDateTime entryBreakStart = LocalDateTime.of(from, LocalTime.of(12, 0, 0));
        final LocalDateTime entryBreakEnd = LocalDateTime.of(toExclusive, LocalTime.of(13, 0, 0));
        final TimeEntryEntity timeEntryBreakEntity = new TimeEntryEntity(2L, "pinguin", "deserved break", entryBreakStart.toInstant(UTC), ZONE_ID_UTC, entryBreakEnd.toInstant(UTC), ZONE_ID_UTC, now, true);

        when(timeEntryRepository.findAllByTouchingPeriod(from.atStartOfDay(UTC).toInstant(), toExclusive.atStartOfDay(UTC).toInstant()))
            .thenReturn(List.of(timeEntryEntity, timeEntryBreakEntity));

        final List<TimeEntry> actual = sut.getEntriesForAllUsers(from, toExclusive);

        final ZonedDateTime expectedStart = ZonedDateTime.of(entryStart, ZONE_ID_UTC);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(entryEnd, ZONE_ID_UTC);

        final ZonedDateTime expectedBreakStart = ZonedDateTime.of(entryBreakStart, ZONE_ID_UTC);
        final ZonedDateTime expectedBreakEnd = ZonedDateTime.of(entryBreakEnd, ZONE_ID_UTC);

        assertThat(actual).containsExactly(
            new TimeEntry(1L, new UserId("batman"), "hard work", expectedStart, expectedEnd, false),
            new TimeEntry(2L, new UserId("pinguin"), "deserved break", expectedBreakStart, expectedBreakEnd, true)
        );
    }

    @Test
    void ensureGetEntriesByUserLocalIds() {

        final UserLocalId batmanLocalId = new UserLocalId(1L);
        final UserLocalId robinLocalId = new UserLocalId(2L);
        final User batman = new User(new UserId("uuid-1"), batmanLocalId, "Bruce", "Wayne", new EMailAddress("batman@example.org"));
        final User robin = new User(new UserId("uuid-2"), robinLocalId, "Dick", "Grayson", new EMailAddress("robin@example.org"));

        when(userManagementService.findAllUsersByLocalIds(List.of(batmanLocalId, robinLocalId))).thenReturn(List.of(batman, robin));

        final Instant now = Instant.now();
        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2023, 2, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(toExclusive, LocalTime.of(12, 0, 0));
        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity(1L, "batman", "hard work", entryStart.toInstant(UTC), ZONE_ID_UTC, entryEnd.toInstant(UTC), ZONE_ID_UTC, now, false);

        final LocalDateTime entryBreakStart = LocalDateTime.of(from, LocalTime.of(12, 0, 0));
        final LocalDateTime entryBreakEnd = LocalDateTime.of(toExclusive, LocalTime.of(13, 0, 0));
        final TimeEntryEntity timeEntryBreakEntity = new TimeEntryEntity(2L, "robin", "deserved break", entryBreakStart.toInstant(UTC), ZONE_ID_UTC, entryBreakEnd.toInstant(UTC), ZONE_ID_UTC, now, true);

        when(timeEntryRepository.findAllByOwnerIsInAndTouchingPeriod(List.of("uuid-1", "uuid-2"), from.atStartOfDay(UTC).toInstant(), toExclusive.atStartOfDay(UTC).toInstant()))
            .thenReturn(List.of(timeEntryEntity, timeEntryBreakEntity));

        final List<TimeEntry> actual = sut.getEntriesByUserLocalIds(from, toExclusive, List.of(batmanLocalId, robinLocalId));

        final ZonedDateTime expectedStart = ZonedDateTime.of(entryStart, ZONE_ID_UTC);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(entryEnd, ZONE_ID_UTC);

        final ZonedDateTime expectedBreakStart = ZonedDateTime.of(entryBreakStart, ZONE_ID_UTC);
        final ZonedDateTime expectedBreakEnd = ZonedDateTime.of(entryBreakEnd, ZONE_ID_UTC);

        assertThat(actual).containsExactly(
            new TimeEntry(1L, new UserId("batman"), "hard work", expectedStart, expectedEnd, false),
            new TimeEntry(2L, new UserId("robin"), "deserved break", expectedBreakStart, expectedBreakEnd, true)
        );
    }

    @Test
    void ensureGetEntries() {

        final LocalDate periodFrom = LocalDate.of(2022, 1, 3);
        final LocalDate periodToExclusive = LocalDate.of(2022, 1, 10);

        final LocalDateTime entryStart = LocalDateTime.of(periodFrom, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(12, 0, 0));
        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity(1L, "batman", "hard work", entryStart.toInstant(UTC), ZoneId.of("UTC"), entryEnd.toInstant(UTC), ZoneId.of("UTC"), Instant.now(), false);

        final LocalDateTime entryBreakStart = LocalDateTime.of(periodFrom, LocalTime.of(12, 0, 0));
        final LocalDateTime entryBreakEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(13, 0, 0));
        final TimeEntryEntity timeEntryBreakEntity = new TimeEntryEntity(2L, "batman", "deserved break", entryBreakStart.toInstant(UTC), ZoneId.of("UTC"), entryBreakEnd.toInstant(UTC), ZoneId.of("UTC"), Instant.now(), true);

        final Instant periodStartInstant = periodFrom.atStartOfDay(UTC).toInstant();
        final Instant periodEndInstant = periodToExclusive.atStartOfDay(UTC).toInstant();
        when(timeEntryRepository.findAllByOwnerAndTouchingPeriod("batman", periodStartInstant, periodEndInstant))
            .thenReturn(List.of(timeEntryEntity, timeEntryBreakEntity));

        final List<TimeEntry> actualEntries = sut.getEntries(periodFrom, periodToExclusive, new UserId("batman"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(entryStart, ZONE_ID_UTC);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(entryEnd, ZONE_ID_UTC);

        final ZonedDateTime expectedBreakStart = ZonedDateTime.of(entryBreakStart, ZONE_ID_UTC);
        final ZonedDateTime expectedBreakEnd = ZonedDateTime.of(entryBreakEnd, ZONE_ID_UTC);

        assertThat(actualEntries).containsExactly(
            new TimeEntry(1L, new UserId("batman"), "hard work", expectedStart, expectedEnd, false),
            new TimeEntry(2L, new UserId("batman"), "deserved break", expectedBreakStart, expectedBreakEnd, true)
        );
    }

    @Test
    void ensureGetEntryWeekPageWithFirstDayOfMonth() {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");

        when(userDateService.firstDayOfWeek(Year.of(2022), 1)).thenReturn(LocalDate.of(2022, 1, 3));

        final ZonedDateTime timeEntryStart = ZonedDateTime.of(2022, 1, 4, 9, 0, 0, 0, zoneIdBerlin);
        final ZonedDateTime timeEntryEnd = ZonedDateTime.of(2022, 1, 4, 12, 0, 0, 0, zoneIdBerlin);
        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity("tenantId", 1L, "batman", "hack the planet!", timeEntryStart.toInstant(), zoneIdBerlin, timeEntryEnd.toInstant(), zoneIdBerlin, Instant.now(), false);

        final ZonedDateTime timeEntryBreakStart = ZonedDateTime.of(2022, 1, 4, 12, 0, 0, 0, zoneIdBerlin);
        final ZonedDateTime timeEntryBreakEnd = ZonedDateTime.of(2022, 1, 4, 13, 0, 0, 0, zoneIdBerlin);
        final TimeEntryEntity timeEntryBreakEntity = new TimeEntryEntity(2L, "batman", "deserved break", timeEntryBreakStart.toInstant(), zoneIdBerlin, timeEntryBreakEnd.toInstant(), zoneIdBerlin, Instant.now(), true);

        final ZonedDateTime fromDateTime = LocalDate.of(2022, 1, 3).atStartOfDay(ZoneId.systemDefault());
        final Instant from = Instant.from(fromDateTime);
        final Instant to = Instant.from(fromDateTime.plusWeeks(1));

        when(timeEntryRepository.findAllByOwnerAndTouchingPeriod("batman", from, to)).thenReturn(List.of(timeEntryEntity, timeEntryBreakEntity));
        when(timeEntryRepository.countAllByOwner("batman")).thenReturn(3L);

        final TimeEntryWeekPage actual = sut.getEntryWeekPage(new UserId("batman"), 2022, 1);

        assertThat(actual).isEqualTo(
            new TimeEntryWeekPage(
                new TimeEntryWeek(
                    LocalDate.of(2022, 1, 3),
                    List.of(new TimeEntryDay(LocalDate.of(2022, 1, 4),
                        List.of(new TimeEntry(2L, new UserId("batman"), "deserved break", timeEntryBreakStart, timeEntryBreakEnd, true),
                        new TimeEntry(1L, new UserId("batman"), "hack the planet!", timeEntryStart, timeEntryEnd, false)))
                    )
                ),
                3
            )
        );
    }


    @Test
    void ensureGetEntryWeekPageWithDaysInCorrectOrder() {

        final ZoneId zoneIdBerlin = ZoneId.of("Europe/Berlin");

        when(userDateService.firstDayOfWeek(Year.of(2023), 5)).thenReturn(LocalDate.of(2023, 1, 30));

        final ZonedDateTime firstDayOfWeekTimeEntryStart = ZonedDateTime.of(2023, 1, 30, 9, 0, 0, 0, zoneIdBerlin);
        final ZonedDateTime firstDayOfWeekTimeEntryEnd = ZonedDateTime.of(2023, 1, 30, 12, 0, 0, 0, zoneIdBerlin);
        final TimeEntryEntity firstDayOfWeekTimeEntry = new TimeEntryEntity("tenantId", 1L, "batman", "hack the planet!", firstDayOfWeekTimeEntryStart.toInstant(), zoneIdBerlin, firstDayOfWeekTimeEntryEnd.toInstant(), zoneIdBerlin, Instant.now(), false);

        final ZonedDateTime lastDayOfWeekTimeEntryStart = ZonedDateTime.of(2023, 2, 5, 9, 0, 0, 0, zoneIdBerlin);
        final ZonedDateTime lastDayOfWeekTimeEntryEnd = ZonedDateTime.of(2023, 2, 5, 12, 0, 0, 0, zoneIdBerlin);
        final TimeEntryEntity lastDayOfWeekTimeEntry = new TimeEntryEntity("tenantId", 2L, "batman", "hack the planet, second time!", lastDayOfWeekTimeEntryStart.toInstant(), zoneIdBerlin, lastDayOfWeekTimeEntryEnd.toInstant(), zoneIdBerlin, Instant.now(), false);


        final ZonedDateTime fromDateTime = LocalDate.of(2023, 1, 30).atStartOfDay(ZoneId.systemDefault());
        final Instant from = Instant.from(fromDateTime);
        final Instant to = Instant.from(fromDateTime.plusWeeks(1));

        when(timeEntryRepository.findAllByOwnerAndTouchingPeriod("batman", from, to)).thenReturn(List.of(lastDayOfWeekTimeEntry, firstDayOfWeekTimeEntry));
        when(timeEntryRepository.countAllByOwner("batman")).thenReturn(6L);

        final TimeEntryWeekPage actual = sut.getEntryWeekPage(new UserId("batman"), 2023, 5);

        assertThat(actual).isEqualTo(
                new TimeEntryWeekPage(
                        new TimeEntryWeek(
                                LocalDate.of(2023, 1, 30),
                                List.of(new TimeEntryDay(LocalDate.of(2023, 2, 5),
                                            List.of(new TimeEntry(2L, new UserId("batman"), "hack the planet, second time!", lastDayOfWeekTimeEntryStart, lastDayOfWeekTimeEntryEnd, false))),
                                        new TimeEntryDay(LocalDate.of(2023, 1, 30),
                                                List.of(new TimeEntry(1L, new UserId("batman"), "hack the planet!", firstDayOfWeekTimeEntryStart, firstDayOfWeekTimeEntryEnd, false)))
                                )
                        ),
                        6
                )
        );
    }
}
