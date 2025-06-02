package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

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

    @BeforeEach
    void setUp() {
        sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clockFixed);
    }

    @Nested
    class GetMinValidTimeEntryDate {

        @Test
        void ensureGetMinValidTimeEntryDateReturnsEmptyWhenNotEnabled() {

            mockSettings(new LockTimeEntriesSettings(false, 1));

            final Optional<LocalDate> actual = sut.getMinValidTimeEntryDate(UTC);
            assertThat(actual).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({
            //                today: 2025-05-29 23:30:00
            "Z,                   0, 2025-05-29",
            "Z,                   1, 2025-05-28",
            "Europe/Berlin,       0, 2025-05-30", // +02:00 to UTC
            "Europe/Berlin,       1, 2025-05-29",
            "Asia/Tokyo,          0, 2025-05-30", // +09:00 to UTC
            "Asia/Tokyo,          1, 2025-05-29",
            "America/Los_Angeles, 0, 2025-05-29", // -07:00 to UTC
            "America/Los_Angeles, 1, 2025-05-28",
        })
        void ensureMinValidTimeEntryDateForGivenUserZoneId(String zoneId, int lockTimeEntriesDaysInPast, String expectedMinValidDate) {

            final Clock clock = Clock.fixed(Instant.parse("2025-05-29T23:30:00Z"), UTC);
            sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clock);

            mockSettings(new LockTimeEntriesSettings(true, lockTimeEntriesDaysInPast));

            final Optional<LocalDate> actual = sut.getMinValidTimeEntryDate(ZoneId.of(zoneId));
            assertThat(actual).hasValue(LocalDate.parse(expectedMinValidDate));
        }
    }

    @Nested
    class IsLocked {

        @Test
        void ensureIsLockedReturnsFalseWhenFeatureIsDisabled() {

            mockSettings(new LockTimeEntriesSettings(false, 1));

            final boolean actual = sut.isLocked(LocalDate.now(clockFixed).minusDays(5));
            assertThat(actual).isFalse();
        }

        @ParameterizedTest
        @CsvSource({
            // today: 2025-05-29
            "0,       2025-05-28",
            "0,       2025-05-27",
            "1,       2025-05-27"
        })
        void ensureIsLockedForLocalDate(int lockTimeEntriesDaysInPast, String date) {

            final Clock clock = Clock.fixed(Instant.parse("2025-05-29T23:30:00Z"), UTC);
            sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clock);

            mockSettings(new LockTimeEntriesSettings(true, lockTimeEntriesDaysInPast));

            final boolean actual = sut.isLocked(LocalDate.parse(date));
            assertThat(actual).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
            // today: 2025-05-29
            "0,       2025-05-30",
            "0,       2025-05-29",
            "1,       2025-05-28",
            "2,       2025-05-27",
        })
        void ensureIsNotLockedForLocalDate(int lockTimeEntriesDaysInPast, String date) {

            final Clock clock = Clock.fixed(Instant.parse("2025-05-29T23:30:00Z"), UTC);
            sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clock);

            mockSettings(new LockTimeEntriesSettings(true, lockTimeEntriesDaysInPast));

            final boolean actual = sut.isLocked(LocalDate.parse(date));
            assertThat(actual).isFalse();
        }

        @ParameterizedTest
        @CsvSource({
            // today: 2025-05-29  23:30:00
            "0,       2025-05-28, 23:59:59",
            "1,       2025-05-27, 23:59:59",
        })
        void ensureIsLockedForLocalDateTime(int lockTimeEntriesDaysInPast, String date, String time) {

            final Clock clock = Clock.fixed(Instant.parse("2025-05-29T23:30:00Z"), UTC);
            sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clock);

            mockSettings(new LockTimeEntriesSettings(true, lockTimeEntriesDaysInPast));

            final LocalDate localDate = LocalDate.parse(date);
            final LocalTime localTime = LocalTime.parse(time);
            final LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);

            final boolean actual = sut.isLocked(localDateTime);
            assertThat(actual).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
            // today: 2025-05-29  23:30:00
            "0,       2025-05-29, 00:00:00",
            "1,       2025-05-28, 00:00:00",
        })
        void ensureIsNotLockedForLocalDateTime(int lockTimeEntriesDaysInPast, String date, String time) {

            final Clock clock = Clock.fixed(Instant.parse("2025-05-29T23:30:00Z"), UTC);
            sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clock);

            mockSettings(new LockTimeEntriesSettings(true, lockTimeEntriesDaysInPast));

            final LocalDate localDate = LocalDate.parse(date);
            final LocalTime localTime = LocalTime.parse(time);
            final LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);

            final boolean actual = sut.isLocked(localDateTime);
            assertThat(actual).isFalse();
        }

        @ParameterizedTest
        @CsvSource({
            //                today: 2025-05-29  23:30:00
            "Z,                   0, 2025-05-28, 23:59:59",
            "Z,                   1, 2025-05-27, 23:59:59",
            "Europe/Berlin,       0, 2025-05-29, 01:59:59", // +02:00 to UTC
            "Europe/Berlin,       1, 2025-05-28, 01:59:59", // +02:00 to UTC
        })
        void ensureIsLockedForZonedDateTime(String zoneId, int lockTimeEntriesDaysInPast, String date, String time) {

            final Clock clock = Clock.fixed(Instant.parse("2025-05-29T23:30:00Z"), UTC);
            sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clock);

            mockSettings(new LockTimeEntriesSettings(true, lockTimeEntriesDaysInPast));

            final LocalDate localDate = LocalDate.parse(date);
            final LocalTime localTime = LocalTime.parse(time);
            final ZonedDateTime zonedDateTime = ZonedDateTime.of(localDate, localTime, ZoneId.of(zoneId));

            final boolean actual = sut.isLocked(zonedDateTime);
            assertThat(actual).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
            //                today: 2025-05-29  23:30:00
            "Z,                   0, 2025-05-29, 00:00:00",
            "Z,                   1, 2025-05-28, 00:00:00",
            "Europe/Berlin,       0, 2025-05-30, 02:00:00", // +02:00 to UTC
            "Europe/Berlin,       1, 2025-05-29, 02:00:00", // +02:00 to UTC
        })
        void ensureIsNotLockedForZonedDateTime(String zoneId, int lockTimeEntriesDaysInPast, String date, String time) {

            final Clock clock = Clock.fixed(Instant.parse("2025-05-29T23:30:00Z"), UTC);
            sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clock);

            mockSettings(new LockTimeEntriesSettings(true, lockTimeEntriesDaysInPast));

            final LocalDate localDate = LocalDate.parse(date);
            final LocalTime localTime = LocalTime.parse(time);
            final ZonedDateTime zonedDateTime = ZonedDateTime.of(localDate, localTime, ZoneId.of(zoneId));

            final boolean actual = sut.isLocked(zonedDateTime);
            assertThat(actual).isFalse();
        }
    }

    @Nested
    class IsTimespanLocked {

        @ParameterizedTest
        @CsvSource({
            // today: 2025-05-29
            "0,       2025-05-28",
            "0,       2025-05-27",
            "1,       2025-05-27"
        })
        void ensureIsTrueForSameLocalDate(int lockTimeEntriesDaysInPast, String date) {

            final Clock clock = Clock.fixed(Instant.parse("2025-05-29T23:30:00Z"), UTC);
            sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clock);

            mockSettings(new LockTimeEntriesSettings(true, lockTimeEntriesDaysInPast));

            final LocalDate localDate = LocalDate.parse(date);

            final boolean actual = sut.isTimespanLocked(localDate, localDate);
            assertThat(actual).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
            // today: 2025-05-29
            "0,       2025-05-30",
            "0,       2025-05-29",
            "1,       2025-05-28",
            "2,       2025-05-27",
        })
        void ensureIsFalseForSameLocalDate(int lockTimeEntriesDaysInPast, String date) {

            final Clock clock = Clock.fixed(Instant.parse("2025-05-29T23:30:00Z"), UTC);
            sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clock);

            mockSettings(new LockTimeEntriesSettings(true, lockTimeEntriesDaysInPast));

            final LocalDate localDate = LocalDate.parse(date);

            final boolean actual = sut.isTimespanLocked(localDate, localDate);
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

        @ParameterizedTest
        @CsvSource({
            //                today: 2025-05-29  23:30:00
            "Z,                   0, 2025-05-28, 23:59:59",
            "Z,                   1, 2025-05-27, 23:59:59",
            "Europe/Berlin,       0, 2025-05-29, 01:59:59", // +02:00 to UTC
            "Europe/Berlin,       1, 2025-05-28, 01:59:59", // +02:00 to UTC
        })
        void ensureIsTrueForSameZonedDateTime(String zoneId, int lockTimeEntriesDaysInPast, String date, String time) {

            final Clock clock = Clock.fixed(Instant.parse("2025-05-29T23:30:00Z"), UTC);
            sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clock);

            mockSettings(new LockTimeEntriesSettings(true, lockTimeEntriesDaysInPast));

            final LocalDate localDate = LocalDate.parse(date);
            final LocalTime localTime = LocalTime.parse(time);
            final ZonedDateTime zonedDateTime = ZonedDateTime.of(localDate, localTime, ZoneId.of(zoneId));

            final boolean actual = sut.isTimespanLocked(zonedDateTime, zonedDateTime);
            assertThat(actual).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
            //                today: 2025-05-29  23:30:00
            "Z,                   0, 2025-05-29, 00:00:00",
            "Z,                   1, 2025-05-28, 00:00:00",
            "Europe/Berlin,       0, 2025-05-30, 02:00:00", // +02:00 to UTC
            "Europe/Berlin,       1, 2025-05-29, 02:00:00", // +02:00 to UTC
        })
        void ensureIsFalseForSameZonedDateTime(String zoneId, int lockTimeEntriesDaysInPast, String date, String time) {

            final Clock clock = Clock.fixed(Instant.parse("2025-05-29T23:30:00Z"), UTC);
            sut = new TimeEntryLockServiceImpl(lockTimeEntriesSettingsService, clock);

            mockSettings(new LockTimeEntriesSettings(true, lockTimeEntriesDaysInPast));

            final LocalDate localDate = LocalDate.parse(date);
            final LocalTime localTime = LocalTime.parse(time);
            final ZonedDateTime zonedDateTime = ZonedDateTime.of(localDate, localTime, ZoneId.of(zoneId));

            final boolean actual = sut.isTimespanLocked(zonedDateTime, zonedDateTime);
            assertThat(actual).isFalse();
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
