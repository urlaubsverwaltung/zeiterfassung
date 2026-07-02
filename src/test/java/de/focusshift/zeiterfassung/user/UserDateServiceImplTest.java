package de.focusshift.zeiterfassung.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDateServiceImplTest {

    private UserDateServiceImpl sut;

    @Mock
    private UserSettingsProvider userSettingsProvider;

    @BeforeEach
    void setUp() {
        sut = new UserDateServiceImpl(userSettingsProvider, Clock.systemUTC());
    }

    static Stream<Arguments> firstDayOfWeekArguments() {
        return Stream.of(
            // 2022-01-01 saturday
            Arguments.of(SUNDAY, LocalDate.of(2022, 1, 1), LocalDate.of(2021, 12, 26)),
            Arguments.of(MONDAY, LocalDate.of(2022, 1, 1), LocalDate.of(2021, 12, 27)),
            Arguments.of(TUESDAY, LocalDate.of(2022, 1, 1), LocalDate.of(2021, 12, 28)),
            Arguments.of(WEDNESDAY, LocalDate.of(2022, 1, 1), LocalDate.of(2021, 12, 29)),
            Arguments.of(THURSDAY, LocalDate.of(2022, 1, 1), LocalDate.of(2021, 12, 30)),
            Arguments.of(FRIDAY, LocalDate.of(2022, 1, 1), LocalDate.of(2021, 12, 31)),
            Arguments.of(SATURDAY, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 1)),
            // 2022-01-02 sunday
            Arguments.of(SUNDAY, LocalDate.of(2022, 1, 2), LocalDate.of(2022, 1, 2)),
            Arguments.of(MONDAY, LocalDate.of(2022, 1, 2), LocalDate.of(2021, 12, 27)),
            Arguments.of(TUESDAY, LocalDate.of(2022, 1, 2), LocalDate.of(2021, 12, 28)),
            Arguments.of(WEDNESDAY, LocalDate.of(2022, 1, 2), LocalDate.of(2021, 12, 29)),
            Arguments.of(THURSDAY, LocalDate.of(2022, 1, 2), LocalDate.of(2021, 12, 30)),
            Arguments.of(FRIDAY, LocalDate.of(2022, 1, 2), LocalDate.of(2021, 12, 31)),
            Arguments.of(SATURDAY, LocalDate.of(2022, 1, 2), LocalDate.of(2022, 1, 1)),
            // 2022-01-03 monday
            Arguments.of(SUNDAY, LocalDate.of(2022, 1, 3), LocalDate.of(2022, 1, 2)),
            Arguments.of(MONDAY, LocalDate.of(2022, 1, 3), LocalDate.of(2022, 1, 3)),
            Arguments.of(TUESDAY, LocalDate.of(2022, 1, 3), LocalDate.of(2021, 12, 28)),
            Arguments.of(WEDNESDAY, LocalDate.of(2022, 1, 3), LocalDate.of(2021, 12, 29)),
            Arguments.of(THURSDAY, LocalDate.of(2022, 1, 3), LocalDate.of(2021, 12, 30)),
            Arguments.of(FRIDAY, LocalDate.of(2022, 1, 3), LocalDate.of(2021, 12, 31)),
            Arguments.of(SATURDAY, LocalDate.of(2022, 1, 3), LocalDate.of(2022, 1, 1)),
            // 2022-01-20 thursday
            Arguments.of(SUNDAY, LocalDate.of(2022, 1, 20), LocalDate.of(2022, 1, 16)),
            Arguments.of(MONDAY, LocalDate.of(2022, 1, 20), LocalDate.of(2022, 1, 17)),
            Arguments.of(TUESDAY, LocalDate.of(2022, 1, 20), LocalDate.of(2022, 1, 18)),
            Arguments.of(WEDNESDAY, LocalDate.of(2022, 1, 20), LocalDate.of(2022, 1, 12)),
            Arguments.of(THURSDAY, LocalDate.of(2022, 1, 20), LocalDate.of(2022, 1, 13)),
            Arguments.of(FRIDAY, LocalDate.of(2022, 1, 20), LocalDate.of(2022, 1, 14)),
            Arguments.of(SATURDAY, LocalDate.of(2022, 1, 20), LocalDate.of(2022, 1, 15)),
            // 2022-09-30 friday
            Arguments.of(SUNDAY, LocalDate.of(2022, 9, 30), LocalDate.of(2022, 9, 25)),
            Arguments.of(MONDAY, LocalDate.of(2022, 9, 30), LocalDate.of(2022, 9, 26)),
            Arguments.of(TUESDAY, LocalDate.of(2022, 9, 30), LocalDate.of(2022, 9, 27)),
            Arguments.of(WEDNESDAY, LocalDate.of(2022, 9, 30), LocalDate.of(2022, 9, 21)),
            Arguments.of(THURSDAY, LocalDate.of(2022, 9, 30), LocalDate.of(2022, 9, 22)),
            Arguments.of(FRIDAY, LocalDate.of(2022, 9, 30), LocalDate.of(2022, 9, 23)),
            Arguments.of(SATURDAY, LocalDate.of(2022, 9, 30), LocalDate.of(2022, 9, 24))
        );
    }

    @ParameterizedTest
    @MethodSource("firstDayOfWeekArguments")
    void ensureLocalDateToFirstDateOfWeek(DayOfWeek firstDayOfWeek, LocalDate pivot, LocalDate expected) {
        when(userSettingsProvider.firstDayOfWeek()).thenReturn(firstDayOfWeek);
        assertThat(sut.localDateToFirstDateOfWeek(pivot)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("firstDayOfWeekArguments")
    void ensureLocalDateToFirstDateOfWeekAdjuster(DayOfWeek firstDayOfWeek, LocalDate pivot, LocalDate expected) {
        when(userSettingsProvider.firstDayOfWeek()).thenReturn(firstDayOfWeek);
        assertThat(pivot.with(sut.localDateToFirstDateOfWeekAdjuster())).isEqualTo(expected);
    }

    @Nested
    class FirstDayOfWeek {

        @ParameterizedTest
        @CsvSource({
            "2024,52,2024-12-23",
            "2025,1,2024-12-30",
            "2025,2,2025-01-06",
        })
        void ensureFirstDayOfWeekForMonday(int year, int week, String expectedDate) {

            when(userSettingsProvider.firstDayOfWeek()).thenReturn(MONDAY);

            final LocalDate actual = sut.firstDayOfWeek(Year.of(year), week);
            assertThat(actual).isEqualTo(LocalDate.parse(expectedDate));
        }

        @ParameterizedTest
        @CsvSource({
            "2024,52,2024-12-22",
            "2025,1,2024-12-29",
            "2025,2,2025-01-05",
        })
        void ensureFirstDayOfWeekForSunday(int year, int week, String expectedDate) {

            when(userSettingsProvider.firstDayOfWeek()).thenReturn(SUNDAY);

            final LocalDate actual = sut.firstDayOfWeek(Year.of(year), week);
            assertThat(actual).isEqualTo(LocalDate.parse(expectedDate));
        }

        @Test
        void ensureFirstDayOfWeekThrowsForInvalidWeek() {

            final Year year = Year.of(2025);

            assertThatThrownBy(() -> sut.firstDayOfWeek(year, 1337))
                .isInstanceOf(DateTimeException.class);
        }

        @ParameterizedTest
        @CsvSource({
            // year boundary where the week-based-year differs from the calendar year -
            // "now" formerly seeded the calculation and could shift the result across midnight / timezone.
            "2024-12-31T23:59:59Z",
            "2025-01-01T00:00:00Z",
            "2025-06-01T12:00:00Z",
        })
        void ensureFirstDayOfWeekIsIndependentOfClockAndTimezone(String instant) {

            when(userSettingsProvider.firstDayOfWeek()).thenReturn(MONDAY);

            // week 1 of 2025 starts on 2024-12-30 (monday), regardless of what "now" is
            final Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
            final UserDateServiceImpl sut = new UserDateServiceImpl(userSettingsProvider, clock);

            assertThat(sut.firstDayOfWeek(Year.of(2025), 1)).isEqualTo(LocalDate.of(2024, 12, 30));
        }
    }

    @Nested
    class GetStartOfWeekDatesForMonth {

        static Stream<Arguments> startOfWeekDatesArguments() {
            return Stream.of(
                // firstDayOfWeek MONDAY - 2021-01 starts on a Friday
                Arguments.of(MONDAY, YearMonth.of(2021, 1), List.of(
                    LocalDate.of(2021, 1, 1),
                    LocalDate.of(2021, 1, 4),
                    LocalDate.of(2021, 1, 11),
                    LocalDate.of(2021, 1, 18),
                    LocalDate.of(2021, 1, 25)
                )),
                // firstDayOfWeek MONDAY - 2021-12 starts on a Wednesday
                Arguments.of(MONDAY, YearMonth.of(2021, 12), List.of(
                    LocalDate.of(2021, 12, 1),
                    LocalDate.of(2021, 12, 6),
                    LocalDate.of(2021, 12, 13),
                    LocalDate.of(2021, 12, 20),
                    LocalDate.of(2021, 12, 27)
                )),
                // firstDayOfWeek MONDAY - 2022-08 starts on a Monday (no duplicate first entry)
                Arguments.of(MONDAY, YearMonth.of(2022, 8), List.of(
                    LocalDate.of(2022, 8, 1),
                    LocalDate.of(2022, 8, 8),
                    LocalDate.of(2022, 8, 15),
                    LocalDate.of(2022, 8, 22),
                    LocalDate.of(2022, 8, 29)
                )),
                // firstDayOfWeek SUNDAY - 2022-01 starts on a Saturday -> first week is a single day
                Arguments.of(SUNDAY, YearMonth.of(2022, 1), List.of(
                    LocalDate.of(2022, 1, 1),
                    LocalDate.of(2022, 1, 2),
                    LocalDate.of(2022, 1, 9),
                    LocalDate.of(2022, 1, 16),
                    LocalDate.of(2022, 1, 23),
                    LocalDate.of(2022, 1, 30)
                )),
                // firstDayOfWeek SUNDAY - 2023-10 starts on a Sunday (no duplicate first entry)
                Arguments.of(SUNDAY, YearMonth.of(2023, 10), List.of(
                    LocalDate.of(2023, 10, 1),
                    LocalDate.of(2023, 10, 8),
                    LocalDate.of(2023, 10, 15),
                    LocalDate.of(2023, 10, 22),
                    LocalDate.of(2023, 10, 29)
                ))
            );
        }

        @ParameterizedTest
        @MethodSource("startOfWeekDatesArguments")
        void ensureStartOfWeekDatesForMonth(DayOfWeek firstDayOfWeek, YearMonth yearMonth, List<LocalDate> expected) {

            when(userSettingsProvider.firstDayOfWeek()).thenReturn(firstDayOfWeek);

            assertThat(sut.getStartOfWeekDatesForMonth(yearMonth)).containsExactlyElementsOf(expected);
        }

        @Test
        void ensureStartOfWeekDatesAlwaysStartWithFirstOfMonth() {

            when(userSettingsProvider.firstDayOfWeek()).thenReturn(MONDAY);

            final LocalDate today = LocalDate.now(Clock.systemUTC());
            final YearMonth yearMonth = YearMonth.from(today);

            assertThat(sut.getStartOfWeekDatesForMonth(yearMonth).getFirst())
                .isEqualTo(today.withDayOfMonth(1));
        }

        @Test
        void ensureStartOfWeekDatesDoNotExtendIntoNextMonth() {

            when(userSettingsProvider.firstDayOfWeek()).thenReturn(MONDAY);

            final YearMonth yearMonth = YearMonth.from(LocalDate.now(Clock.systemUTC()));

            assertThat(sut.getStartOfWeekDatesForMonth(yearMonth))
                .allMatch(date -> YearMonth.from(date).equals(yearMonth));
        }
    }

    @Nested
    class Today {

        @Test
        void ensureTodayIsProjectedIntoUsersTimezoneAcrossMidnight() {

            // 23:30 UTC -> already 01:30 on the next day in Europe/Berlin (+2 in summer)
            final Clock clock = Clock.fixed(Instant.parse("2026-07-02T23:30:00Z"), ZoneOffset.UTC);
            final UserDateServiceImpl sut = new UserDateServiceImpl(userSettingsProvider, clock);

            when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("Europe/Berlin"));

            assertThat(sut.today()).isEqualTo(LocalDate.of(2026, 7, 3));
        }

        @Test
        void ensureTodayIsProjectedIntoUsersTimezoneWestOfUtc() {

            // 02:00 UTC -> still 22:00 on the previous day in America/New_York (-4 in summer)
            final Clock clock = Clock.fixed(Instant.parse("2026-07-02T02:00:00Z"), ZoneOffset.UTC);
            final UserDateServiceImpl sut = new UserDateServiceImpl(userSettingsProvider, clock);

            when(userSettingsProvider.zoneId()).thenReturn(ZoneId.of("America/New_York"));

            assertThat(sut.today()).isEqualTo(LocalDate.of(2026, 7, 1));
        }
    }
}
