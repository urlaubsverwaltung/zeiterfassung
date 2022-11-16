package de.focusshift.zeiterfassung.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.stream.Stream;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDateServiceImplTest {

    private UserDateServiceImpl sut;

    @Mock
    private UserSettingsProvider userSettingsProvider;

    @BeforeEach
    void setUp() {
        sut = new UserDateServiceImpl(userSettingsProvider);
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
}
