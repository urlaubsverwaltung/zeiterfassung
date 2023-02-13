package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeClockServiceTest {

    private final ZoneId ZONED_ID_BERLIN = ZoneId.of("Europe/Berlin");

    private TimeClockService sut;

    @Mock
    private TimeClockRepository timeClockRepository;
    @Mock
    private TimeEntryService timeEntryService;
    @Mock
    private UserSettingsProvider userSettingsProvider;

    @BeforeEach
    void setUp() {
        sut = new TimeClockService(timeClockRepository, timeEntryService, userSettingsProvider);
    }

    @Test
    void ensureCurrentTimeClockReturnsEmptyOptional() {

        when(timeClockRepository.findByOwnerAndStoppedAtIsNull("batman")).thenReturn(Optional.empty());

        assertThat(sut.getCurrentTimeClock(new UserId("batman"))).isEmpty();
    }

    @Test
    void ensureCurrentTimeClockIsReturned() {

        final ZoneId utc = ZoneId.of("UTC");
        final Instant stoppedAt = Instant.now();
        final Instant startedAt = stoppedAt.minusSeconds(120);

        final TimeClockEntity runningTimeClockEntity = TimeClockEntity.builder()
            .id(1L)
            .owner("batman")
            .startedAt(startedAt)
            .startedAtZoneId(utc)
            // running time clock with a stoppedAt value makes no sens, actually.
            // however, this makes testing easier. alternative would be to inject a clock.
            .stoppedAt(stoppedAt)
            .stoppedAtZoneId(utc)
            .comment("")
            .build();

        when(timeClockRepository.findByOwnerAndStoppedAtIsNull("batman")).thenReturn(Optional.of(runningTimeClockEntity));

        final Optional<TimeClock> actualMaybeTimeClock = sut.getCurrentTimeClock(new UserId("batman"));
        assertThat(actualMaybeTimeClock).isPresent();

        final ZonedDateTime startedAtZonedDT = ZonedDateTime.ofInstant(startedAt, utc);
        final ZonedDateTime stoppedAtZonedDT = ZonedDateTime.ofInstant(stoppedAt, utc);
        final TimeClock actualTimeClock = actualMaybeTimeClock.get();
        assertThat(actualTimeClock).isEqualTo(new TimeClock(1L, new UserId("batman"), startedAtZonedDT, "", false, Optional.of(stoppedAtZonedDT)));
    }

    @Test
    void ensureStartTimeClockPersistsNewEntity() {

        when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("UTC"));

        sut.startTimeClock(new UserId("batman"));

        final ArgumentCaptor<TimeClockEntity> timeClockEntityArgumentCaptor = ArgumentCaptor.forClass(TimeClockEntity.class);

        verify(timeClockRepository).save(timeClockEntityArgumentCaptor.capture());
        final TimeClockEntity persistedTimeClockEntity = timeClockEntityArgumentCaptor.getValue();

        assertThat(persistedTimeClockEntity.getId()).isNull();
        assertThat(persistedTimeClockEntity.getOwner()).isEqualTo("batman");
        assertThat(persistedTimeClockEntity.getStartedAt().truncatedTo(MINUTES)).isEqualTo(Instant.now().truncatedTo(MINUTES));
        assertThat(persistedTimeClockEntity.getStoppedAt()).isNull();
    }

    @Test
    void ensureStopTimeClockDoesNothingWhenThereIsNothingRunningCurrently() {

        when(timeClockRepository.findByOwnerAndStoppedAtIsNull("batman")).thenReturn(Optional.empty());

        sut.stopTimeClock(new UserId("batman"));

        verifyNoMoreInteractions(timeClockRepository);
        verifyNoInteractions(timeEntryService);
    }

    @Test
    void ensureStopTimeClockStopsTheCurrentRunningTimeClock() {

        final ZoneId utc = ZoneId.of("UTC");
        final Instant now = Instant.now();
        final Instant startedAt = now.minusSeconds(120);

        final TimeClockEntity runningTimeClockEntity = TimeClockEntity.builder()
            .id(1L)
            .owner("batman")
            .startedAt(startedAt)
            .startedAtZoneId(utc)
            .stoppedAt(null)
            .stoppedAtZoneId(null)
            .comment("awesome comment")
            .build();

        when(timeClockRepository.findByOwnerAndStoppedAtIsNull("batman"))
            .thenReturn(Optional.of(runningTimeClockEntity));

        when(timeClockRepository.save(any()))
            .thenReturn(
                TimeClockEntity.builder()
                    .id(1L)
                    .owner("batman")
                    .startedAt(startedAt)
                    .startedAtZoneId(utc)
                    .stoppedAt(now)
                    .stoppedAtZoneId(utc)
                    .comment("awesome comment")
                    .build()
            );

        when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("Europe/Berlin"));

        sut.stopTimeClock(new UserId("batman"));

        final ArgumentCaptor<TimeClockEntity> argumentCaptor = ArgumentCaptor.forClass(TimeClockEntity.class);

        verify(timeClockRepository).save(argumentCaptor.capture());
        final TimeClockEntity entity = argumentCaptor.getValue();

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getOwner()).isEqualTo("batman");
        assertThat(entity.getStartedAt()).isEqualTo(startedAt);
        assertThat(entity.getStoppedAt().truncatedTo(MINUTES)).isEqualTo(now.truncatedTo(MINUTES));
        assertThat(entity.getComment()).isEqualTo("awesome comment");
    }

    @Test
    void ensureStopTimeClockCreatesANewTimeEntryForTheStoppedTimeClock() {

        final Instant now = Instant.now();
        final Instant startedAtInstant = now.minusSeconds(120);
        final ZonedDateTime startedAt = ZonedDateTime.ofInstant(startedAtInstant, ZONED_ID_BERLIN);
        final ZonedDateTime stoppedAt = ZonedDateTime.ofInstant(now, ZONED_ID_BERLIN);

        final TimeClockEntity runningTimeClockEntity = TimeClockEntity.builder()
            .id(1L)
            .owner("batman")
            .startedAt(startedAt.toInstant())
            .startedAtZoneId(startedAt.getZone())
            .comment("awesome comment")
            .isBreak(true)
            .build();

        when(timeClockRepository.findByOwnerAndStoppedAtIsNull("batman")).thenReturn(Optional.of(runningTimeClockEntity));

        final TimeClockEntity stoppedTimeClockEntity = TimeClockEntity.builder(runningTimeClockEntity)
            .stoppedAt(stoppedAt.toInstant())
            .stoppedAtZoneId(stoppedAt.getZone())
            .build();

        when(timeClockRepository.save(runningTimeClockEntity)).thenReturn(stoppedTimeClockEntity);

        when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("Europe/Berlin"));

        sut.stopTimeClock(new UserId("batman"));

        verify(timeEntryService).createTimeEntry(new UserId("batman"), "awesome comment", startedAt, stoppedAt, true);
    }

    @Test
    void ensureUpdateTimeClock() throws Exception {

        final TimeClockEntity runningTimeClockEntity = TimeClockEntity.builder()
            .id(1L)
            .owner("batman")
            .startedAt(Instant.now())
            .startedAtZoneId(ZoneId.of("Europe/Amsterdam"))
            .build();

        when(timeClockRepository.findByOwnerAndStoppedAtIsNull("batman")).thenReturn(Optional.of(runningTimeClockEntity));

        when(timeClockRepository.save(any(TimeClockEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final ZoneId zoneId = ZoneId.of("Europe/Berlin");
        final ZonedDateTime date = ZonedDateTime.of(2023, 1, 11, 13, 37, 0, 0, zoneId);

        final TimeClock actualUpdatedTimeClock = sut.updateTimeClock(userId, new TimeClockUpdate(userId, date, "awesome comment", true));

        assertThat(actualUpdatedTimeClock.userId()).isEqualTo(userId);
        assertThat(actualUpdatedTimeClock.startedAt()).isEqualTo(date);
        assertThat(actualUpdatedTimeClock.stoppedAt()).isEmpty();
        assertThat(actualUpdatedTimeClock.comment()).isEqualTo("awesome comment");

        final ArgumentCaptor<TimeClockEntity> entityCaptor = ArgumentCaptor.forClass(TimeClockEntity.class);
        verify(timeClockRepository).save(entityCaptor.capture());

        assertThat(entityCaptor.getValue()).satisfies(entity -> {
            assertThat(entity.getOwner()).isEqualTo("batman");
            assertThat(entity.getComment()).isEqualTo("awesome comment");
            assertThat(entity.isBreak()).isTrue();
            assertThat(entity.getStartedAt()).isEqualTo(Instant.from(date));
            assertThat(entity.getStartedAtZoneId()).isEqualTo("Europe/Berlin");
            assertThat(entity.getStoppedAt()).isNull();
            assertThat(entity.getStoppedAtZoneId()).isNull();
        });
    }

    @Test
    void ensureUpdateTimeClockThrowsWhenThereIsNoClockRunning() {

        final UserId userId = new UserId("batman");
        final TimeClockUpdate timeClockUpdate = new TimeClockUpdate(userId, null, "", false);

        when(timeClockRepository.findByOwnerAndStoppedAtIsNull("batman")).thenReturn(Optional.empty());

        assertThatExceptionOfType(TimeClockNotStartedException.class)
            .isThrownBy(() -> sut.updateTimeClock(userId, timeClockUpdate));
    }
}
