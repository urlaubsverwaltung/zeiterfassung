package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.timeentry.events.DayLockedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    private static final Clock clock = Clock.fixed(Instant.parse("2023-06-15T10:00:00Z"), ZoneId.of("Europe/Berlin"));

    private SettingsService sut;

    @Mock
    private FederalStateSettingsRepository federalStateSettingsRepository;
    @Mock
    private LockTimeEntriesSettingsRepository lockTimeEntriesSettingsRepository;
    @Mock
    private SubtractBreakFromTimeEntrySettingsRepository subtractBreakFromTimeEntrySettingsRepository;
    @Mock
    private TimeEntrySettingsRepository timeEntrySettingsRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;


    @BeforeEach
    void setUp() {
        sut = new SettingsService(
            federalStateSettingsRepository,
            lockTimeEntriesSettingsRepository,
            subtractBreakFromTimeEntrySettingsRepository,
            timeEntrySettingsRepository,
            applicationEventPublisher,
            clock
        );
    }

    @Nested
    class LockTimeEntriesSettingsTest {

        @Test
        void ensureGetLockTimeEntriesSettings() {

            final LockTimeEntriesSettingsEntity entity = new LockTimeEntriesSettingsEntity();
            entity.setId(1L);
            entity.setLockingIsActive(true);
            entity.setLockTimeEntriesDaysInPast(7);

            when(lockTimeEntriesSettingsRepository.findAll()).thenReturn(List.of(entity));

            final LockTimeEntriesSettings actual = sut.getLockTimeEntriesSettings();
            assertThat(actual.lockingIsActive()).isTrue();
            assertThat(actual.lockTimeEntriesDaysInPast()).isEqualTo(7);
        }

        @Test
        void ensureGetLockTimeEntriesSettingsReturnsDefaultSettings() {

            when(lockTimeEntriesSettingsRepository.findAll()).thenReturn(List.of());

            final LockTimeEntriesSettings actual = sut.getLockTimeEntriesSettings();
            assertThat(actual.lockingIsActive()).isFalse();
            assertThat(actual.lockTimeEntriesDaysInPast()).isEqualTo(2);
        }

        @Test
        void ensureUpdateLockTimeEntriesSettings() {

            final LockTimeEntriesSettingsEntity entity = new LockTimeEntriesSettingsEntity();
            entity.setId(1L);
            entity.setLockingIsActive(false);
            entity.setLockTimeEntriesDaysInPast(1);

            when(lockTimeEntriesSettingsRepository.findAll()).thenReturn(List.of(entity));
            when(lockTimeEntriesSettingsRepository.save(any(LockTimeEntriesSettingsEntity.class))).thenAnswer(returnsFirstArg());

            final LockTimeEntriesSettings actual = sut.updateLockTimeEntriesSettings(true, 42);

            assertThat(actual.lockingIsActive()).isTrue();
            assertThat(actual.lockTimeEntriesDaysInPast()).isEqualTo(42);

            final ArgumentCaptor<LockTimeEntriesSettingsEntity> captor = ArgumentCaptor.forClass(LockTimeEntriesSettingsEntity.class);
            verify(lockTimeEntriesSettingsRepository).save(captor.capture());

            assertThat(captor.getValue()).isSameAs(entity);
            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.isLockingIsActive()).isTrue();
            assertThat(entity.getLockTimeEntriesDaysInPast()).isEqualTo(42);
        }

        @Test
        void ensureUpdateLockTimeEntriesSettingsCreatesNewEntity() {

            when(lockTimeEntriesSettingsRepository.findAll()).thenReturn(List.of());
            when(lockTimeEntriesSettingsRepository.save(any(LockTimeEntriesSettingsEntity.class))).thenAnswer(returnsFirstArg());

            final LockTimeEntriesSettings actual = sut.updateLockTimeEntriesSettings(true, 42);

            assertThat(actual.lockingIsActive()).isTrue();
            assertThat(actual.lockTimeEntriesDaysInPast()).isEqualTo(42);

            final ArgumentCaptor<LockTimeEntriesSettingsEntity> captor = ArgumentCaptor.forClass(LockTimeEntriesSettingsEntity.class);
            verify(lockTimeEntriesSettingsRepository).save(captor.capture());

            assertThat(captor.getValue()).satisfies(entity -> {
                assertThat(entity.getId()).isNull(); // set by JPA
                assertThat(entity.isLockingIsActive()).isTrue();
                assertThat(entity.getLockTimeEntriesDaysInPast()).isEqualTo(42);
            });
        }

        @Test
        void ensureToNotSendDayLockedEventsWhenIsLockingIsDeactivated() {

            final LockTimeEntriesSettingsEntity entity = new LockTimeEntriesSettingsEntity();
            entity.setId(1L);
            entity.setLockingIsActive(true);
            entity.setLockTimeEntriesDaysInPast(10);

            when(lockTimeEntriesSettingsRepository.findAll()).thenReturn(List.of(entity));
            when(lockTimeEntriesSettingsRepository.save(any(LockTimeEntriesSettingsEntity.class))).thenAnswer(returnsFirstArg());

            final LockTimeEntriesSettings actual = sut.updateLockTimeEntriesSettings(false, 9);

            assertThat(actual.lockingIsActive()).isFalse();
            assertThat(actual.lockTimeEntriesDaysInPast()).isEqualTo(9);

            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        void ensureToSendDayLockedEventsWhenLockTimeEntriesDaysInPastWillBeShortened() {

            final LockTimeEntriesSettingsEntity entity = new LockTimeEntriesSettingsEntity();
            entity.setId(1L);
            entity.setLockingIsActive(true);
            entity.setLockTimeEntriesDaysInPast(10);

            when(lockTimeEntriesSettingsRepository.findAll()).thenReturn(List.of(entity));
            when(lockTimeEntriesSettingsRepository.save(any(LockTimeEntriesSettingsEntity.class))).thenAnswer(returnsFirstArg());

            final LockTimeEntriesSettings actual = sut.updateLockTimeEntriesSettings(true, 9);

            assertThat(actual.lockingIsActive()).isTrue();
            assertThat(actual.lockTimeEntriesDaysInPast()).isEqualTo(9);

            final ZoneId zoneId = ZoneId.of("Europe/Berlin");
            final LocalDate today = LocalDate.now(clock.withZone(zoneId));

            verify(applicationEventPublisher).publishEvent(new DayLockedEvent(today.minusDays(10), zoneId));
        }

        @Test
        void ensureToSendDayLockedEventsWhenLockTimeEntriesDaysInPastWillBeTheSame() {

            final LockTimeEntriesSettingsEntity entity = new LockTimeEntriesSettingsEntity();
            entity.setId(1L);
            entity.setLockingIsActive(true);
            entity.setLockTimeEntriesDaysInPast(10);

            when(lockTimeEntriesSettingsRepository.findAll()).thenReturn(List.of(entity));
            when(lockTimeEntriesSettingsRepository.save(any(LockTimeEntriesSettingsEntity.class))).thenAnswer(returnsFirstArg());

            final LockTimeEntriesSettings actual = sut.updateLockTimeEntriesSettings(true, 10);

            assertThat(actual.lockingIsActive()).isTrue();
            assertThat(actual.lockTimeEntriesDaysInPast()).isEqualTo(10);

            final ZoneId zoneId = ZoneId.of("Europe/Berlin");
            final LocalDate today = LocalDate.now(clock.withZone(zoneId));

            verify(applicationEventPublisher).publishEvent(new DayLockedEvent(today.minusDays(11), zoneId));
        }

        @Test
        void ensureExceptionForOneDayLockedEventDoesNotAbortRemainingDates() {

            final LockTimeEntriesSettingsEntity entity = new LockTimeEntriesSettingsEntity();
            entity.setId(1L);
            entity.setLockingIsActive(true);
            entity.setLockTimeEntriesDaysInPast(2);

            when(lockTimeEntriesSettingsRepository.findAll()).thenReturn(List.of(entity));
            when(lockTimeEntriesSettingsRepository.save(any(LockTimeEntriesSettingsEntity.class))).thenAnswer(returnsFirstArg());

            final ZoneId zoneId = ZoneId.of("Europe/Berlin");
            final LocalDate today = LocalDate.now(clock.withZone(zoneId));

            doThrow(new IllegalStateException("boom"))
                .when(applicationEventPublisher).publishEvent(new DayLockedEvent(today.minusDays(2), zoneId));

            sut.updateLockTimeEntriesSettings(true, 0);

            verify(applicationEventPublisher).publishEvent(new DayLockedEvent(today.minusDays(3), zoneId));
            verify(applicationEventPublisher).publishEvent(new DayLockedEvent(today.minusDays(2), zoneId));
            verify(applicationEventPublisher).publishEvent(new DayLockedEvent(today.minusDays(1), zoneId));
        }

        @Test
        void ensureToNotSendDayLockedEventsWhenLockTimeEntriesDaysInPastWillBeEnlarged() {

            final LockTimeEntriesSettingsEntity entity = new LockTimeEntriesSettingsEntity();
            entity.setId(1L);
            entity.setLockingIsActive(true);
            entity.setLockTimeEntriesDaysInPast(1);

            when(lockTimeEntriesSettingsRepository.findAll()).thenReturn(List.of(entity));
            when(lockTimeEntriesSettingsRepository.save(any(LockTimeEntriesSettingsEntity.class))).thenAnswer(returnsFirstArg());

            final LockTimeEntriesSettings actual = sut.updateLockTimeEntriesSettings(true, 2);

            assertThat(actual.lockingIsActive()).isTrue();
            assertThat(actual.lockTimeEntriesDaysInPast()).isEqualTo(2);

            verifyNoInteractions(applicationEventPublisher);
        }
    }

    @Nested
    class SubtractBreakFromTimeEntrySettingsTest {

        @Test
        void ensureGetSubtractBreakFromTimeEntrySettings() {

            final Instant enabledTimestamp = Instant.now();

            final SubtractBreakFromTimeEntrySettingsEntity entity = new SubtractBreakFromTimeEntrySettingsEntity();
            entity.setId(1L);
            entity.setSubtractBreakFromTimeEntryIsActive(true);
            entity.setSubtractBreakFromTimeEntryEnabledTimestamp(enabledTimestamp);

            when(subtractBreakFromTimeEntrySettingsRepository.findAll()).thenReturn(List.of(entity));

            final SubtractBreakFromTimeEntrySettings expected =
                new SubtractBreakFromTimeEntrySettings(true, Optional.of(enabledTimestamp));

            final Optional<SubtractBreakFromTimeEntrySettings> actual = sut.getSubtractBreakFromTimeEntrySettings();
            assertThat(actual).hasValue(expected);
        }

        @Test
        void ensureGetSubtractBreakFromTimeEntrySettingsReturnsDefaultSettings() {

            when(subtractBreakFromTimeEntrySettingsRepository.findAll()).thenReturn(List.of());

            assertThat(sut.getSubtractBreakFromTimeEntrySettings()).isEmpty();
        }

        @Test
        void ensureUpdateDeactivatesSettingAndRemovesTimestamp() {
            final SubtractBreakFromTimeEntrySettingsEntity entity = new SubtractBreakFromTimeEntrySettingsEntity();
            entity.setId(1L);
            entity.setSubtractBreakFromTimeEntryIsActive(true);
            entity.setSubtractBreakFromTimeEntryEnabledTimestamp(Instant.now());

            when(subtractBreakFromTimeEntrySettingsRepository.findAll()).thenReturn(List.of(entity));
            when(subtractBreakFromTimeEntrySettingsRepository.save(any(SubtractBreakFromTimeEntrySettingsEntity.class))).thenAnswer(returnsFirstArg());

            final SubtractBreakFromTimeEntrySettings result = sut.updateSubtractBreakFromTimeEntrySettings(false, null);

            assertThat(result.subtractBreakFromTimeEntryIsActive()).isFalse();
            assertThat(result.subtractBreakFromTimeEntryEnabledTimestamp()).isEmpty();
        }

        @Test
        void ensureUpdateCreatesNewEntityIfNoneExists() {
            when(subtractBreakFromTimeEntrySettingsRepository.findAll()).thenReturn(List.of());
            when(subtractBreakFromTimeEntrySettingsRepository.save(any(SubtractBreakFromTimeEntrySettingsEntity.class))).thenAnswer(returnsFirstArg());

            final Instant timestamp = Instant.now();

            final SubtractBreakFromTimeEntrySettings result = sut.updateSubtractBreakFromTimeEntrySettings(true, timestamp);

            assertThat(result.subtractBreakFromTimeEntryIsActive()).isTrue();
            assertThat(result.subtractBreakFromTimeEntryEnabledTimestamp()).hasValue(timestamp);

            verify(subtractBreakFromTimeEntrySettingsRepository).save(assertArg(entity -> assertThat(entity.getId()).isNull()));
        }

        @Test
        void ensureUpdateDoesNotChangeTimestampIfStateUnchanged() {
            final Instant timestamp = Instant.now();
            final SubtractBreakFromTimeEntrySettingsEntity entity = new SubtractBreakFromTimeEntrySettingsEntity();
            entity.setId(1L);
            entity.setSubtractBreakFromTimeEntryIsActive(true);
            entity.setSubtractBreakFromTimeEntryEnabledTimestamp(timestamp);

            when(subtractBreakFromTimeEntrySettingsRepository.findAll()).thenReturn(List.of(entity));
            when(subtractBreakFromTimeEntrySettingsRepository.save(any(SubtractBreakFromTimeEntrySettingsEntity.class))).thenAnswer(returnsFirstArg());

            final SubtractBreakFromTimeEntrySettings result = sut.updateSubtractBreakFromTimeEntrySettings(true, timestamp);

            assertThat(result.subtractBreakFromTimeEntryIsActive()).isTrue();
            assertThat(result.subtractBreakFromTimeEntryEnabledTimestamp()).hasValue(timestamp);
        }
    }

    @Nested
    class TimeEntrySettingsTest {

        @Test
        void ensureGetTimeEntrySettings() {

            final TimeEntrySettingsEntity entity = new TimeEntrySettingsEntity();
            entity.setId(1L);
            entity.setCommentEnabled(false);
            entity.setBreakIntegrated(true);
            entity.setDefaultBreakMinutes(30);

            when(timeEntrySettingsRepository.findAll()).thenReturn(List.of(entity));

            final TimeEntrySettings actual = sut.getTimeEntrySettings();
            assertThat(actual.commentEnabled()).isFalse();
            assertThat(actual.breakIntegrated()).isTrue();
            assertThat(actual.defaultBreakMinutes()).isEqualTo(30);
        }

        @Test
        void ensureGetTimeEntrySettingsReturnsDefaultSettings() {

            when(timeEntrySettingsRepository.findAll()).thenReturn(List.of());

            final TimeEntrySettings actual = sut.getTimeEntrySettings();
            assertThat(actual).isEqualTo(TimeEntrySettings.DEFAULT);
        }

        @Test
        void ensureUpdateTimeEntrySettings() {

            final TimeEntrySettingsEntity entity = new TimeEntrySettingsEntity();
            entity.setId(1L);
            entity.setCommentEnabled(true);
            entity.setBreakIntegrated(false);
            entity.setDefaultBreakMinutes(45);

            when(timeEntrySettingsRepository.findAll()).thenReturn(List.of(entity));
            when(timeEntrySettingsRepository.save(any(TimeEntrySettingsEntity.class))).thenAnswer(returnsFirstArg());

            final TimeEntrySettings actual = sut.updateTimeEntrySettings(false, true, 20);

            assertThat(actual.commentEnabled()).isFalse();
            assertThat(actual.breakIntegrated()).isTrue();
            assertThat(actual.defaultBreakMinutes()).isEqualTo(20);

            final ArgumentCaptor<TimeEntrySettingsEntity> captor = ArgumentCaptor.forClass(TimeEntrySettingsEntity.class);
            verify(timeEntrySettingsRepository).save(captor.capture());

            assertThat(captor.getValue()).isSameAs(entity);
            assertThat(entity.isCommentEnabled()).isFalse();
            assertThat(entity.isBreakIntegrated()).isTrue();
            assertThat(entity.getDefaultBreakMinutes()).isEqualTo(20);
        }

        @Test
        void ensureUpdateTimeEntrySettingsCreatesNewEntity() {

            when(timeEntrySettingsRepository.findAll()).thenReturn(List.of());
            when(timeEntrySettingsRepository.save(any(TimeEntrySettingsEntity.class))).thenAnswer(returnsFirstArg());

            final TimeEntrySettings actual = sut.updateTimeEntrySettings(true, false, 45);

            assertThat(actual.commentEnabled()).isTrue();
            assertThat(actual.breakIntegrated()).isFalse();
            assertThat(actual.defaultBreakMinutes()).isEqualTo(45);

            final ArgumentCaptor<TimeEntrySettingsEntity> captor = ArgumentCaptor.forClass(TimeEntrySettingsEntity.class);
            verify(timeEntrySettingsRepository).save(captor.capture());

            assertThat(captor.getValue()).satisfies(entity -> {
                assertThat(entity.getId()).isNull(); // set by JPA
                assertThat(entity.isCommentEnabled()).isTrue();
                assertThat(entity.isBreakIntegrated()).isFalse();
                assertThat(entity.getDefaultBreakMinutes()).isEqualTo(45);
            });
        }
    }
}
