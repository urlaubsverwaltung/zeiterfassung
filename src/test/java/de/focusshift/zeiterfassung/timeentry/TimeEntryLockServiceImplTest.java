package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeEntryLockServiceImplTest {

    private TimeEntryLockServiceImpl sut;

    @Mock
    private LockTimeEntriesSettingsService lockTimeEntriesSettingsService;

    private static final Clock clockFixed = Clock.fixed(Clock.systemUTC().instant(), UTC);
    private static final ZoneId ZONE_TOKYO = ZoneId.of("Asia/Tokyo");

    @BeforeEach
    void setUp() {
        sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clockFixed);
    }

    @Nested
    class IsTimespanLocked {

        @Test
        void ensureTimespanNotLockedForSameDay() {

            mockSettings(new LockTimeEntriesSettings(true, 0));

            final LocalDate today = LocalDate.now(clockFixed);

            final boolean actual = sut.isTimespanLocked(today, today);
            assertThat(actual).isFalse();
        }

        @Test
        void ensureTimespanNotLockedForStartTodayEndOnBoundary() {

            mockSettings(new LockTimeEntriesSettings(true, 1));

            final LocalDate today = LocalDate.now(clockFixed);

            final boolean actual = sut.isTimespanLocked(today, today.minusDays(1));
            assertThat(actual).isFalse();
        }

        @Test
        void ensureTimespanNotLockedForInBetween() {

            mockSettings(new LockTimeEntriesSettings(true, 7));

            final LocalDate yesterday = LocalDate.now(clockFixed).minusDays(1);

            final boolean actual = sut.isTimespanLocked(yesterday, yesterday.minusDays(1));
            assertThat(actual).isFalse();
        }

        @Test
        void ensureTimespanNotLockedWhenSettingIsInactive() {

            mockSettings(new LockTimeEntriesSettings(false, 0));

            final LocalDate ooooold = LocalDate.now(clockFixed).minusYears(1);

            final boolean actual = sut.isTimespanLocked(ooooold, ooooold);
            assertThat(actual).isFalse();
        }

        @Test
        void ensureIsTimespanLockedWhenStartIsToOld() {

            mockSettings(new LockTimeEntriesSettings(true, 0));

            final LocalDate yesterday = LocalDate.now(clockFixed).minusDays(1);

            final boolean actual = sut.isTimespanLocked(yesterday, yesterday);
            assertThat(actual).isTrue();
        }

        @Test
        void ensureIsTimespanLockedWhenEndIsToOld() {

            mockSettings(new LockTimeEntriesSettings(true, 0));

            final LocalDate today = LocalDate.now(clockFixed);

            final boolean actual = sut.isTimespanLocked(today, today.minusDays(42));
            assertThat(actual).isTrue();
        }

        @Test
        void ensureTimespanNotLockedForOtherTimeZone() {

            mockSettings(new LockTimeEntriesSettings(true, 0));

            final ZonedDateTime todayOtherTimeZone = ZonedDateTime.now(clockFixed).withZoneSameLocal(ZONE_TOKYO);

            final boolean actual = sut.isTimespanLocked(todayOtherTimeZone, todayOtherTimeZone);
            assertThat(actual).isFalse();
        }

        @Test
        void ensureTimespanLockedForOtherTimeZone() {

            mockSettings(new LockTimeEntriesSettings(true, 0));

            final ZonedDateTime yesterdayOtherTimezone = ZonedDateTime.now(clockFixed).withZoneSameLocal(ZONE_TOKYO).minusDays(1);

            final boolean actual = sut.isTimespanLocked(yesterdayOtherTimezone, yesterdayOtherTimezone);
            assertThat(actual).isTrue();
        }
    }

    @Nested
    class IsUserAllowedToBypassLock {

        @ParameterizedTest
        @EnumSource(value = SecurityRole.class, names = {"ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL"}, mode = EnumSource.Mode.EXCLUDE)
        void ensureIsUserAllowedToBypassLockReturnFalse(SecurityRole role) {
            final boolean actual = sut.isUserAllowedToBypassLock(List.of(role));
            assertThat(actual).isFalse();
        }

        @Test
        void ensureIsUserAllowedToBypassLockReturnFalse() {
            final boolean actual = sut.isUserAllowedToBypassLock(List.of(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL));
            assertThat(actual).isTrue();
        }
    }

    private void mockSettings(LockTimeEntriesSettings settings) {
        when(lockTimeEntriesSettingsService.getLockTimeEntriesSettings()).thenReturn(settings);
    }
}
