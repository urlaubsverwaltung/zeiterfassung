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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    private SettingsService sut;

    @Mock
    private FederalStateSettingsRepository federalStateSettingsRepository;
    @Mock
    private LockTimeEntriesSettingsRepository lockTimeEntriesSettingsRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @BeforeEach
    void setUp() {
        sut = new SettingsService(federalStateSettingsRepository, lockTimeEntriesSettingsRepository, applicationEventPublisher);
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
            final LocalDate today = LocalDate.now(zoneId);

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
            final LocalDate today = LocalDate.now(zoneId);

            verify(applicationEventPublisher).publishEvent(new DayLockedEvent(today.minusDays(11), zoneId));
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
}
