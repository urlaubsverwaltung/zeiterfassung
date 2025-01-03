package de.focusshift.zeiterfassung.user;

import org.springframework.stereotype.Service;
import org.threeten.extra.YearWeek;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.WeekFields;

import static java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR;
import static java.time.temporal.TemporalAdjusters.previousOrSame;
import static java.util.Locale.GERMANY;

@Service
class UserDateServiceImpl implements UserDateService {

    private final UserSettingsProvider userSettingsProvider;
    private final Clock clock;

    public UserDateServiceImpl(UserSettingsProvider userSettingsProvider, Clock clock) {
        this.userSettingsProvider = userSettingsProvider;
        this.clock = clock;
    }

    @Override
    public TemporalAdjuster localDateToFirstDateOfWeekAdjuster() {
        final DayOfWeek usersFirstDayOfWeek = userSettingsProvider.firstDayOfWeek();
        return temporal -> this.localDateToFirstDateOfWeek(LocalDate.from(temporal), usersFirstDayOfWeek);
    }

    @Override
    public LocalDate localDateToFirstDateOfWeek(LocalDate localDate) {
        final DayOfWeek usersFirstDayOfWeek = userSettingsProvider.firstDayOfWeek();
        return localDateToFirstDateOfWeek(localDate, usersFirstDayOfWeek);
    }

    @Override
    public LocalDate firstDayOfWeek(Year year, int weekOfYear) {

        // `YearWeek.of` throws when the weekOfYear is invalid
        YearWeek.of(year, weekOfYear);

        final int minimalDaysInFirstWeek = WeekFields.of(GERMANY).getMinimalDaysInFirstWeek();
        final DayOfWeek usersFirstDayOfWeek = userSettingsProvider.firstDayOfWeek();
        final WeekFields weekFields = WeekFields.of(usersFirstDayOfWeek, minimalDaysInFirstWeek);

        return LocalDate.now(clock)
            .withYear(year.getValue())
            .with(weekFields.weekOfWeekBasedYear(), weekOfYear)
            .with(previousOrSame(usersFirstDayOfWeek));
    }

    private LocalDate localDateToFirstDateOfWeek(LocalDate localDate, DayOfWeek firstDayOfWeek) {
        // using minimalDaysInFirstWeek = 4 since it is defined by ISO-8601 (starting week with monday)
        // I have no glue whether this value can be use here or not. Some unit tests say we can... so...
        final WeekFields userWeekFields = WeekFields.of(firstDayOfWeek, 4);

        final int temporalYear = localDate.getYear();
        final int temporalWeekOfYear = localDate.get(WEEK_OF_WEEK_BASED_YEAR);

        final LocalDate previousOrSame = localDate.with(previousOrSame(firstDayOfWeek));

        final int year = previousOrSame.getYear();
        final int week = previousOrSame.get(userWeekFields.weekOfWeekBasedYear());
        if (year == temporalYear && week > temporalWeekOfYear) {
            return previousOrSame.minusWeeks(1);
        }

        return previousOrSame;
    }
}
