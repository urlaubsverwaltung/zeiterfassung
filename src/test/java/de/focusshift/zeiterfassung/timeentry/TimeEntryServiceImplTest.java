package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.usermanagement.WorkingTimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
    private WorkingTimeService workingTimeService;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private UserSettingsProvider userSettingsProvider;

    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, clock);
    }

    @Test
    void ensureFindTimeEntry() {

        final LocalDateTime entryStart = LocalDateTime.of(LocalDate.of(2023, 2, 25), LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(LocalDate.of(2023, 2, 25), LocalTime.of(12, 0, 0));

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

        final Optional<TimeEntry> actual = sut.findTimeEntry(42L);
        assertThat(actual).isPresent();
        assertThat(actual.get()).satisfies(timeEntry -> {
            assertThat(timeEntry.id()).isEqualTo(new TimeEntryId(42L));
            assertThat(timeEntry.userId()).isEqualTo(new UserId("batman"));
            assertThat(timeEntry.comment()).isEmpty();
            assertThat(timeEntry.start()).isEqualTo(ZonedDateTime.of(entryStart, ZONE_ID_UTC));
            assertThat(timeEntry.end()).isEqualTo(ZonedDateTime.of(entryEnd, ZONE_ID_UTC));
            assertThat(timeEntry.isBreak()).isFalse();
            assertThat(timeEntry.workDuration().value()).isEqualTo(Duration.ofHours(2));
        });
    }

    @Test
    void ensureFindTimeEntryReturnsEmptyOptional() {
        when(timeEntryRepository.findById(42L)).thenReturn(Optional.empty());
        assertThat(sut.findTimeEntry(42L)).isEmpty();
    }

    @Test
    void ensureCreateTimeEntry() {

        final Instant now = Instant.now();
        final Clock fixedClock = Clock.fixed(now, UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, fixedClock);

        final LocalDateTime entryStart = LocalDateTime.of(2023, 1, 1, 10, 0, 0);
        final LocalDateTime entryEnd = LocalDateTime.of(2023, 1, 1, 12, 0, 0);

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
        final TimeEntryId id = new TimeEntryId(42L);
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> sut.updateTimeEntry(id, "", null, null, null, false));
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

        assertThatExceptionOfType(TimeEntryUpdateNotPlausibleException.class).isThrownBy(
            () -> sut.updateTimeEntry(new TimeEntryId(42L), "", now, now, duration, false)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryStart(boolean isBreak) throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, fixedClock);

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
            isBreak);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime newStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC).plusMinutes(30);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration sameDuration = Duration.ofHours(2);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, sameEnd, sameDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(sameEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(Duration.ofMinutes(90));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(sameEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(sameEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryStartAndEnd(boolean isBreak) throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, fixedClock);

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
            isBreak);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime newStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC).minusMinutes(30);
        final ZonedDateTime newEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC).plusMinutes(30);
        final Duration sameDuration = Duration.ofHours(2);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, newEnd, sameDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(Duration.ofHours(3));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryStartAndDuration(boolean isBreak) throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, fixedClock);

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
            isBreak);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime newStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC).minusMinutes(30);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration newDuration = Duration.ofHours(3);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, sameEnd, newDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newStart.plusHours(3));
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(Duration.ofHours(3));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newStart.plusHours(3).toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(sameEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryEnd(boolean isBreak) throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, fixedClock);

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
            isBreak);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime newEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC).minusMinutes(30);
        final Duration sameDuration = Duration.ofHours(2);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, newEnd, sameDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(sameStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(Duration.ofMinutes(90));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(sameStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryEndAndDuration(boolean isBreak) throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, fixedClock);

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
            isBreak);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime newEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC).minusMinutes(30);
        final Duration newDuration = Duration.ofHours(3);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, newEnd, newDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newEnd.minusHours(3));
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(newDuration);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(newEnd.minusHours(3).toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryDuration(boolean isBreak) throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, fixedClock);

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
            isBreak);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration newDuration = Duration.ofHours(4);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, sameEnd, newDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(sameStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(sameStart.plusMinutes(newDuration.toMinutes()));
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(newDuration);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(sameStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(sameStart.plusMinutes(newDuration.toMinutes()).toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @Test
    void ensureUpdateTimeEntryWhenStartEndDurationAreGivenAndPlausible() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, fixedClock);

        final LocalDate date = LocalDate.of(2023, 2, 27);
        final LocalDateTime previousStart = LocalDateTime.of(date, LocalTime.of(15, 0, 0));
        final LocalDateTime previousEnd = LocalDateTime.of(date, LocalTime.of(16, 0, 0));

        final TimeEntryEntity entity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            previousStart.toInstant(UTC),
            ZONE_ID_UTC,
            previousEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(entity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime newStart = previousStart.minusHours(1).atZone(ZONE_ID_UTC);
        final ZonedDateTime newEnd = previousEnd.plusHours(1).atZone(ZONE_ID_UTC);
        final Duration newDuration = Duration.ofHours(3);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, newEnd, newDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userId()).isEqualTo(new UserId("batman"));
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(newDuration);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(fixedClock));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @Test
    void ensureUpdateTimeEntryStartWithGivenEndAndDuration() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, fixedClock);

        final LocalDate date = LocalDate.of(2023, 2, 27);
        final LocalDateTime previousStart = LocalDateTime.of(date, LocalTime.of(15, 0, 0));
        final LocalDateTime previousEnd = LocalDateTime.of(date, LocalTime.of(16, 0, 0));

        final TimeEntryEntity entity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            previousStart.toInstant(UTC),
            ZONE_ID_UTC,
            previousEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(entity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime newEnd = previousEnd.plusHours(1).atZone(ZONE_ID_UTC);
        final Duration newDuration = Duration.ofHours(3);

        sut.updateTimeEntry(new TimeEntryId(42L), "", null, newEnd, newDuration, false);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();
        final ZonedDateTime expectedStart = previousStart.minusHours(1).atZone(ZONE_ID_UTC);

        assertThat(actualPersisted.getStart()).isEqualTo(expectedStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(expectedStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
    }

    @Test
    void ensureUpdateTimeEntryEndWithGivenStartAndDuration() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, fixedClock);

        final LocalDate date = LocalDate.of(2023, 2, 27);
        final LocalDateTime previousStart = LocalDateTime.of(date, LocalTime.of(15, 0, 0));
        final LocalDateTime previousEnd = LocalDateTime.of(date, LocalTime.of(16, 0, 0));

        final TimeEntryEntity entity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            previousStart.toInstant(UTC),
            ZONE_ID_UTC,
            previousEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(entity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime newStart = previousStart.minusHours(1).atZone(ZONE_ID_UTC);
        final Duration newDuration = Duration.ofHours(3);

        sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, null, newDuration, false);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();
        final ZonedDateTime expectedEnd = previousEnd.plusHours(1).atZone(ZONE_ID_UTC);

        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(expectedEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(expectedEnd.getZone().getId());
    }

    static Stream<Duration> emptyDuration() {
        return Stream.of(null, Duration.ZERO);
    }

    @ParameterizedTest
    @MethodSource("emptyDuration")
    void ensureUpdateTimeEntryStartAndEndWithoutDuration(Duration givenDuration) throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, fixedClock);

        final LocalDate date = LocalDate.of(2023, 2, 27);
        final LocalDateTime previousStart = LocalDateTime.of(date, LocalTime.of(15, 0, 0));
        final LocalDateTime previousEnd = LocalDateTime.of(date, LocalTime.of(16, 0, 0));

        final TimeEntryEntity entity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            previousStart.toInstant(UTC),
            ZONE_ID_UTC,
            previousEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(entity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final ZonedDateTime newStart = previousStart.minusHours(1).atZone(ZONE_ID_UTC);
        final ZonedDateTime newEnd = previousEnd.plusHours(1).atZone(ZONE_ID_UTC);

        sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, newEnd, givenDuration, false);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
    }

    @Test
    void ensureUpdateTimeEntryIsBreak() throws Exception {

        final Clock fixedClock = Clock.fixed(Instant.now(), UTC);
        sut = new TimeEntryServiceImpl(timeEntryRepository, userManagementService, workingTimeService, userDateService, userSettingsProvider, fixedClock);

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
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(sameStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(sameEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isTrue();
        assertThat(actualUpdatedTimeEntry.workDuration().value()).isEqualTo(Duration.ZERO);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
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

        when(timeEntryRepository.findAllByStartGreaterThanEqualAndStartLessThan(from.atStartOfDay(UTC).toInstant(), toExclusive.atStartOfDay(UTC).toInstant()))
            .thenReturn(List.of(timeEntryEntity, timeEntryBreakEntity));

        final UserLocalId batmanLocalId = new UserLocalId(1L);
        final UserLocalId pinguinLocalId = new UserLocalId(2L);
        final User batman = new User(new UserId("batman"), batmanLocalId, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User pinguin = new User(new UserId("pinguin"), pinguinLocalId, "ping", "uin", new EMailAddress("pinguin@example.org"), Set.of());

        when(userManagementService.findAllUsersByIds(Set.of(new UserId("batman"), new UserId("pinguin"))))
            .thenReturn(List.of(batman, pinguin));

        final Map<UserLocalId, List<TimeEntry>> actual = sut.getEntriesForAllUsers(from, toExclusive);

        final ZonedDateTime expectedStart = ZonedDateTime.of(entryStart, ZONE_ID_UTC);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(entryEnd, ZONE_ID_UTC);

        final ZonedDateTime expectedBreakStart = ZonedDateTime.of(entryBreakStart, ZONE_ID_UTC);
        final ZonedDateTime expectedBreakEnd = ZonedDateTime.of(entryBreakEnd, ZONE_ID_UTC);

        assertThat(actual)
            .hasSize(2)
            .hasEntrySatisfying(batmanLocalId, timeEntries -> {
                assertThat(timeEntries).containsExactly(
                    new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hard work", expectedStart, expectedEnd, false)
                );
            })
            .hasEntrySatisfying(pinguinLocalId, timeEntries -> {
                assertThat(timeEntries).containsExactly(
                    new TimeEntry(new TimeEntryId(2L), new UserId("pinguin"), "deserved break", expectedBreakStart, expectedBreakEnd, true)
                );
            });
    }

    @Test
    void ensureGetEntriesByUserLocalIds() {

        final UserLocalId batmanLocalId = new UserLocalId(1L);
        final UserLocalId robinLocalId = new UserLocalId(2L);
        final User batman = new User(new UserId("uuid-1"), batmanLocalId, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User robin = new User(new UserId("uuid-2"), robinLocalId, "Dick", "Grayson", new EMailAddress("robin@example.org"), Set.of());

        when(userManagementService.findAllUsersByLocalIds(List.of(batmanLocalId, robinLocalId))).thenReturn(List.of(batman, robin));

        final Instant now = Instant.now();
        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2023, 2, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(toExclusive, LocalTime.of(12, 0, 0));
        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity(1L, "uuid-1", "hard work", entryStart.toInstant(UTC), ZONE_ID_UTC, entryEnd.toInstant(UTC), ZONE_ID_UTC, now, false);

        final LocalDateTime entryBreakStart = LocalDateTime.of(from, LocalTime.of(12, 0, 0));
        final LocalDateTime entryBreakEnd = LocalDateTime.of(toExclusive, LocalTime.of(13, 0, 0));
        final TimeEntryEntity timeEntryBreakEntity = new TimeEntryEntity(2L, "uuid-2", "deserved break", entryBreakStart.toInstant(UTC), ZONE_ID_UTC, entryBreakEnd.toInstant(UTC), ZONE_ID_UTC, now, true);

        when(timeEntryRepository.findAllByOwnerIsInAndStartGreaterThanEqualAndStartLessThan(List.of("uuid-1", "uuid-2"), from.atStartOfDay(UTC).toInstant(), toExclusive.atStartOfDay(UTC).toInstant()))
            .thenReturn(List.of(timeEntryEntity, timeEntryBreakEntity));

        final Map<UserLocalId, List<TimeEntry>> actual = sut.getEntriesByUserLocalIds(from, toExclusive, List.of(batmanLocalId, robinLocalId));

        final ZonedDateTime expectedStart = ZonedDateTime.of(entryStart, ZONE_ID_UTC);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(entryEnd, ZONE_ID_UTC);

        final ZonedDateTime expectedBreakStart = ZonedDateTime.of(entryBreakStart, ZONE_ID_UTC);
        final ZonedDateTime expectedBreakEnd = ZonedDateTime.of(entryBreakEnd, ZONE_ID_UTC);

        assertThat(actual)
            .hasSize(2)
            .hasEntrySatisfying(batmanLocalId, timeEntries -> {
                assertThat(timeEntries).containsExactly(
                    new TimeEntry(new TimeEntryId(1L), new UserId("uuid-1"), "hard work", expectedStart, expectedEnd, false)
                );
            })
            .hasEntrySatisfying(robinLocalId, timeEntries -> {
                assertThat(timeEntries).containsExactly(
                    new TimeEntry(new TimeEntryId(2L), new UserId("uuid-2"), "deserved break", expectedBreakStart, expectedBreakEnd, true)
                );
            });
    }

    @Test
    void ensureGetEntriesByUserLocalIdsReturnsValuesForEveryAskedUserLocalId() {

        final UserLocalId batmanLocalId = new UserLocalId(1L);

        when(userManagementService.findAllUsersByLocalIds(List.of(batmanLocalId))).thenReturn(List.of());

        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2023, 2, 1);

        when(timeEntryRepository.findAllByOwnerIsInAndStartGreaterThanEqualAndStartLessThan(List.of(), from.atStartOfDay(UTC).toInstant(), toExclusive.atStartOfDay(UTC).toInstant()))
            .thenReturn(List.of());

        final Map<UserLocalId, List<TimeEntry>> actual = sut.getEntriesByUserLocalIds(from, toExclusive, List.of(batmanLocalId));

        assertThat(actual)
            .hasSize(1)
            .hasEntrySatisfying(batmanLocalId, timeEntries -> {
                assertThat(timeEntries).isEmpty();
            });
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
        when(timeEntryRepository.findAllByOwnerAndStartGreaterThanEqualAndStartLessThan("batman", periodStartInstant, periodEndInstant))
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

        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

        when(userDateService.firstDayOfWeek(Year.of(2022), 1)).thenReturn(LocalDate.of(2022, 1, 3));

        final ZonedDateTime timeEntryStart = ZonedDateTime.of(2022, 1, 4, 9, 0, 0, 0, userZoneId);
        final ZonedDateTime timeEntryEnd = ZonedDateTime.of(2022, 1, 4, 12, 0, 0, 0, userZoneId);
        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity("tenantId", 1L, "batman", "hack the planet!", timeEntryStart.toInstant(), userZoneId, timeEntryEnd.toInstant(), userZoneId, Instant.now(), false);

        final ZonedDateTime timeEntryBreakStart = ZonedDateTime.of(2022, 1, 4, 12, 0, 0, 0, userZoneId);
        final ZonedDateTime timeEntryBreakEnd = ZonedDateTime.of(2022, 1, 4, 13, 0, 0, 0, userZoneId);
        final TimeEntryEntity timeEntryBreakEntity = new TimeEntryEntity(2L, "batman", "deserved break", timeEntryBreakStart.toInstant(), userZoneId, timeEntryBreakEnd.toInstant(), userZoneId, Instant.now(), true);

        final ZonedDateTime fromDateTime = LocalDate.of(2022, 1, 3).atStartOfDay(userZoneId);
        final Instant from = Instant.from(fromDateTime);
        final Instant to = Instant.from(fromDateTime.plusWeeks(1));

        when(timeEntryRepository.findAllByOwnerAndStartGreaterThanEqualAndStartLessThan("batman", from, to))
            .thenReturn(List.of(timeEntryEntity, timeEntryBreakEntity));

        when(timeEntryRepository.countAllByOwner("batman")).thenReturn(3L);

        final User batman = new User(new UserId("batman"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findUserById(new UserId("batman"))).thenReturn(Optional.of(batman));

        when(workingTimeService.getWorkingHoursByUserAndYearWeek(new UserLocalId(1337L), Year.of(2022), 1))
            .thenReturn(Map.of(
                LocalDate.of(2022, 1, 3), PlannedWorkingHours.EIGHT,
                LocalDate.of(2022, 1, 4), PlannedWorkingHours.EIGHT,
                LocalDate.of(2022, 1, 5), PlannedWorkingHours.EIGHT,
                LocalDate.of(2022, 1, 6), PlannedWorkingHours.EIGHT,
                LocalDate.of(2022, 1, 7), PlannedWorkingHours.EIGHT,
                LocalDate.of(2022, 1, 8), PlannedWorkingHours.ZERO, // saturday
                LocalDate.of(2022, 1, 9), PlannedWorkingHours.ZERO  // sunday
            ));

        final TimeEntryWeekPage actual = sut.getEntryWeekPage(new UserId("batman"), 2022, 1);

        assertThat(actual).isEqualTo(
            new TimeEntryWeekPage(
                new TimeEntryWeek(
                    LocalDate.of(2022, 1, 3),
                    new PlannedWorkingHours(Duration.ofHours(40)),
                    List.of(
                        new TimeEntryDay(
                            LocalDate.of(2022, 1, 4),
                            PlannedWorkingHours.EIGHT,
                            List.of(
                                new TimeEntry(new TimeEntryId(2L), new UserId("batman"), "deserved break", timeEntryBreakStart, timeEntryBreakEnd, true),
                                new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hack the planet!", timeEntryStart, timeEntryEnd, false)
                            )
                        )
                    )
                ),
                3
            )
        );
    }

    @Test
    void ensureGetEntryWeekPageWithDaysInCorrectOrder() {

        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

        when(userDateService.firstDayOfWeek(Year.of(2023), 5)).thenReturn(LocalDate.of(2023, 1, 30));

        final ZonedDateTime firstDayOfWeekTimeEntryStart = ZonedDateTime.of(2023, 1, 30, 9, 0, 0, 0, userZoneId);
        final ZonedDateTime firstDayOfWeekTimeEntryEnd = ZonedDateTime.of(2023, 1, 30, 12, 0, 0, 0, userZoneId);
        final TimeEntryEntity firstDayOfWeekTimeEntry = new TimeEntryEntity("tenantId", 1L, "batman", "hack the planet!", firstDayOfWeekTimeEntryStart.toInstant(), userZoneId, firstDayOfWeekTimeEntryEnd.toInstant(), userZoneId, Instant.now(), false);

        final ZonedDateTime lastDayOfWeekTimeEntryStart = ZonedDateTime.of(2023, 2, 5, 9, 0, 0, 0, userZoneId);
        final ZonedDateTime lastDayOfWeekTimeEntryEnd = ZonedDateTime.of(2023, 2, 5, 12, 0, 0, 0, userZoneId);
        final TimeEntryEntity lastDayOfWeekTimeEntry = new TimeEntryEntity("tenantId", 2L, "batman", "hack the planet, second time!", lastDayOfWeekTimeEntryStart.toInstant(), userZoneId, lastDayOfWeekTimeEntryEnd.toInstant(), userZoneId, Instant.now(), false);


        final ZonedDateTime fromDateTime = LocalDate.of(2023, 1, 30).atStartOfDay(userZoneId);
        final Instant from = Instant.from(fromDateTime);
        final Instant to = Instant.from(fromDateTime.plusWeeks(1));

        when(timeEntryRepository.findAllByOwnerAndStartGreaterThanEqualAndStartLessThan("batman", from, to))
            .thenReturn(List.of(lastDayOfWeekTimeEntry, firstDayOfWeekTimeEntry));

        when(timeEntryRepository.countAllByOwner("batman")).thenReturn(6L);

        final User batman = new User(new UserId("batman"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findUserById(new UserId("batman"))).thenReturn(Optional.of(batman));

        // Map<LocalDate, PlannedWorkingHours>
        when(workingTimeService.getWorkingHoursByUserAndYearWeek(new UserLocalId(1337L), Year.of(2023), 5))
            .thenReturn(Map.of(
                LocalDate.of(2023, 1, 30), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 1, 31), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 2, 1), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 2, 2), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 2, 3), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 2, 4), PlannedWorkingHours.ZERO,
                LocalDate.of(2023, 2, 5), PlannedWorkingHours.ZERO
            ));

        final TimeEntryWeekPage actual = sut.getEntryWeekPage(new UserId("batman"), 2023, 5);

        assertThat(actual).isEqualTo(
            new TimeEntryWeekPage(
                new TimeEntryWeek(
                    LocalDate.of(2023, 1, 30),
                    new PlannedWorkingHours(Duration.ofHours(40)),
                    List.of(
                        new TimeEntryDay(
                            LocalDate.of(2023, 2, 5),
                            PlannedWorkingHours.ZERO,
                            List.of(
                                new TimeEntry(new TimeEntryId(2L), new UserId("batman"), "hack the planet, second time!", lastDayOfWeekTimeEntryStart, lastDayOfWeekTimeEntryEnd, false)
                            )
                        ),
                        new TimeEntryDay(
                            LocalDate.of(2023, 1, 30),
                            PlannedWorkingHours.EIGHT,
                            List.of(
                                new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hack the planet!", firstDayOfWeekTimeEntryStart, firstDayOfWeekTimeEntryEnd, false)
                            )
                        )
                    )
                ),
                6
            )
        );
    }

    @Test
    void ensureGetEntryWeekPageWithTimeEntryActuallyNotInWeekOfYear() {

        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

        when(userDateService.firstDayOfWeek(Year.of(2023), 24)).thenReturn(LocalDate.of(2023, 6, 12));

        final ZonedDateTime firstDateOfWeek = LocalDate.of(2023, 6, 12).atStartOfDay(userZoneId);
        final Instant from = Instant.from(firstDateOfWeek);
        final Instant to = Instant.from(firstDateOfWeek.plusWeeks(1));

        when(timeEntryRepository.findAllByOwnerAndStartGreaterThanEqualAndStartLessThan("batman", from, to))
            .thenReturn(List.of());

        when(timeEntryRepository.countAllByOwner("batman")).thenReturn(6L);

        final User batman = new User(new UserId("batman"), new UserLocalId(1337L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        when(userManagementService.findUserById(new UserId("batman"))).thenReturn(Optional.of(batman));

        // Map<LocalDate, PlannedWorkingHours>
        when(workingTimeService.getWorkingHoursByUserAndYearWeek(new UserLocalId(1337L), Year.of(2023), 24))
            .thenReturn(Map.of(
                LocalDate.of(2023, 6, 12), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 6, 13), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 6, 14), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 6, 15), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 6, 16), PlannedWorkingHours.EIGHT,
                LocalDate.of(2023, 6, 17), PlannedWorkingHours.ZERO,
                LocalDate.of(2023, 6, 18), PlannedWorkingHours.ZERO
            ));

        final TimeEntryWeekPage actual = sut.getEntryWeekPage(new UserId("batman"), 2023, 24);

        assertThat(actual).isEqualTo(
            new TimeEntryWeekPage(
                new TimeEntryWeek(
                    LocalDate.of(2023, 6, 12),
                    new PlannedWorkingHours(Duration.ofHours(40)),
                    List.of()
                ),
                6
            )
        );
    }
}
