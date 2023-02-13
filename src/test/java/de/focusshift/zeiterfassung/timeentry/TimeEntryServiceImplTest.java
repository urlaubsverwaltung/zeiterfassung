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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
    void ensureCreateTimeEntry() {

        final Instant now = Instant.now();
        final Clock fixedClock = Clock.fixed(now, UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, userDateService, fixedClock);

        final LocalDateTime entryStart = LocalDateTime.of(2023, 1, 1, 10, 0,0);
        final LocalDateTime entryEnd = LocalDateTime.of(2023, 1, 1, 12, 0,0);

        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(invocationOnMock -> {
            final TimeEntryEntity entity = invocationOnMock.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        final TimeEntry actual = sut.createTimeEntry(
            new UserId("batman"),
            "hard work",
            ZonedDateTime.of(entryStart, ZONE_ID_UTC),
            ZonedDateTime.of(entryEnd, ZONE_ID_UTC),
            false
        );

        assertThat(actual).isEqualTo(new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hard work", ZonedDateTime.of(entryStart, ZONE_ID_UTC), ZonedDateTime.of(entryEnd, ZONE_ID_UTC), false));

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
    void ensureUpdateTimeEntryThrowsWhenTimeEntryIsUnknown() {
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
            () -> sut.updateTimeEntry(new TimeEntryId(42L), null, null, null, null, false)
        );
    }

    @Test
    void ensureUpdateTimeEntryThrowsWhenStarEndDurationAreGiven() {

        final ZonedDateTime now = ZonedDateTime.now(clock);
        final Duration duration = Duration.ofMinutes(60);

        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2023, 2, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(toExclusive, LocalTime.of(12, 0, 0));

        final TimeEntryEntity entity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(entity));

        assertThatExceptionOfType(TimeEntryUpdateException.class).isThrownBy(
            () -> sut.updateTimeEntry(new TimeEntryId(42L), null, now, now, duration, false)
        );
    }

    @Test
    void ensureUpdateTimeEntryStart() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, userDateService, fixedClock);

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime newStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC).plusMinutes(30);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration sameDuration = Duration.ofHours(2);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, sameEnd, sameDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEqualTo("");
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(sameEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(Duration.ofMinutes(90));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEqualTo("");
        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(sameEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(sameEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @Test
    void ensureUpdateTimeEntryStartAndEnd() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, userDateService, fixedClock);

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime newStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC).minusMinutes(30);
        final ZonedDateTime newEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC).plusMinutes(30);
        final Duration sameDuration = Duration.ofHours(2);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, newEnd, sameDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEqualTo("");
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(Duration.ofHours(3));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEqualTo("");
        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @Test
    void ensureUpdateTimeEntryStartAndDuration() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, userDateService, fixedClock);

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime newStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC).minusMinutes(30);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration newDuration = Duration.ofHours(3);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, sameEnd, newDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEqualTo("");
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newStart.plusHours(3));
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(Duration.ofHours(3));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEqualTo("");
        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newStart.plusHours(3).toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(sameEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @Test
    void ensureUpdateTimeEntryEnd() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, userDateService, fixedClock);

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime newEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC).minusMinutes(30);
        final Duration sameDuration = Duration.ofHours(2);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, newEnd, sameDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEqualTo("");
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(sameStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(Duration.ofMinutes(90));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEqualTo("");
        assertThat(actualPersisted.getStart()).isEqualTo(sameStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @Test
    void ensureUpdateTimeEntryEndAndDuration() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, userDateService, fixedClock);

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime newEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC).minusMinutes(30);
        final Duration newDuration = Duration.ofHours(3);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, newEnd, newDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEqualTo("");
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newEnd.minusHours(3));
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(newDuration);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEqualTo("");
        assertThat(actualPersisted.getStart()).isEqualTo(newEnd.minusHours(3).toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @Test
    void ensureUpdateTimeEntryDuration() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, userDateService, fixedClock);

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration newDuration = Duration.ofHours(4);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, sameEnd, newDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEqualTo("");
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(sameStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(sameStart.plusMinutes(newDuration.toMinutes()));
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(newDuration);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEqualTo("");
        assertThat(actualPersisted.getStart()).isEqualTo(sameStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(sameStart.plusMinutes(newDuration.toMinutes()).toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @Test
    void ensureUpdateTimeEntryIsBreak() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, userDateService, fixedClock);

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration sameDuration = Duration.ofHours(2);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, sameEnd, sameDuration, true);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEqualTo("");
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(sameStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(sameEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isTrue();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(Duration.ZERO);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEqualTo("");
        assertThat(actualPersisted.getStart()).isEqualTo(sameStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(sameEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(sameEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isTrue();
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
            new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hard work", expectedStart, expectedEnd, false),
            new TimeEntry(new TimeEntryId(2L), new UserId("pinguin"), "deserved break", expectedBreakStart, expectedBreakEnd, true)
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
            new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hard work", expectedStart, expectedEnd, false),
            new TimeEntry(new TimeEntryId(2L), new UserId("robin"), "deserved break", expectedBreakStart, expectedBreakEnd, true)
        );
    }

    @Test
    void ensureGetEntriesSortedByStart_NewestFirst() {

        final LocalDate periodFrom = LocalDate.of(2022, 1, 3);
        final LocalDate periodToExclusive = LocalDate.of(2022, 1, 10);

        final LocalDateTime entryStart = LocalDateTime.of(periodFrom, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(12, 0, 0));
        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity(1L, "batman", "hard work", entryStart.toInstant(UTC), ZoneId.of("UTC"), entryEnd.toInstant(UTC), ZoneId.of("UTC"), Instant.now(), false);

        final LocalDateTime entryBreakStart = LocalDateTime.of(periodFrom, LocalTime.of(12, 0, 0));
        final LocalDateTime entryBreakEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(13, 0, 0));
        final TimeEntryEntity timeEntryBreakEntity = new TimeEntryEntity(2L, "batman", "deserved break", entryBreakStart.toInstant(UTC), ZoneId.of("UTC"), entryBreakEnd.toInstant(UTC), ZoneId.of("UTC"), Instant.now(), true);

        final LocalDateTime entryStart2 = LocalDateTime.of(periodFrom, LocalTime.of(8, 0, 0));
        final LocalDateTime entryEnd2 = LocalDateTime.of(periodToExclusive, LocalTime.of(8, 30, 0));
        final TimeEntryEntity timeEntryEntity2 = new TimeEntryEntity(3L, "batman", "waking up *zzzz", entryStart2.toInstant(UTC), ZoneId.of("UTC"), entryEnd2.toInstant(UTC), ZoneId.of("UTC"), Instant.now(), false);

        final Instant periodStartInstant = periodFrom.atStartOfDay(UTC).toInstant();
        final Instant periodEndInstant = periodToExclusive.atStartOfDay(UTC).toInstant();
        when(timeEntryRepository.findAllByOwnerAndTouchingPeriod("batman", periodStartInstant, periodEndInstant))
            .thenReturn(List.of(timeEntryEntity, timeEntryBreakEntity, timeEntryEntity2));

        final List<TimeEntry> actualEntries = sut.getEntries(periodFrom, periodToExclusive, new UserId("batman"));

        final ZonedDateTime expectedStart = ZonedDateTime.of(entryStart, ZONE_ID_UTC);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(entryEnd, ZONE_ID_UTC);

        final ZonedDateTime expectedBreakStart = ZonedDateTime.of(entryBreakStart, ZONE_ID_UTC);
        final ZonedDateTime expectedBreakEnd = ZonedDateTime.of(entryBreakEnd, ZONE_ID_UTC);

        final ZonedDateTime expectedStart2 = ZonedDateTime.of(entryStart2, ZONE_ID_UTC);
        final ZonedDateTime expectedEnd2 = ZonedDateTime.of(entryEnd2, ZONE_ID_UTC);

        assertThat(actualEntries).containsExactly(
            new TimeEntry(new TimeEntryId(2L), new UserId("batman"), "deserved break", expectedBreakStart, expectedBreakEnd, true),
            new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hard work", expectedStart, expectedEnd, false),
            new TimeEntry(new TimeEntryId(3L), new UserId("batman"), "waking up *zzzz", expectedStart2, expectedEnd2, false)
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
                        List.of(new TimeEntry(new TimeEntryId(2L), new UserId("batman"), "deserved break", timeEntryBreakStart, timeEntryBreakEnd, true),
                        new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hack the planet!", timeEntryStart, timeEntryEnd, false)))
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
                                            List.of(new TimeEntry(new TimeEntryId(2L), new UserId("batman"), "hack the planet, second time!", lastDayOfWeekTimeEntryStart, lastDayOfWeekTimeEntryEnd, false))),
                                        new TimeEntryDay(LocalDate.of(2023, 1, 30),
                                                List.of(new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hack the planet!", firstDayOfWeekTimeEntryStart, firstDayOfWeekTimeEntryEnd, false)))
                                )
                        ),
                        6
                )
        );
    }
}
