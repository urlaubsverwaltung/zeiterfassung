package de.focusshift.zeiterfassung.user;

import org.springframework.stereotype.Service;
import org.threeten.extra.YearWeek;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.WeekFields;

import static java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

@Service
class UserDateServiceImpl implements UserDateService {

    private final UserSettingsProvider userSettingsProvider;

    public UserDateServiceImpl(UserSettingsProvider userSettingsProvider) {
        this.userSettingsProvider = userSettingsProvider;
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
        final YearWeek yearWeek = YearWeek.of(year, weekOfYear);

        final DayOfWeek usersFirstDayOfWeek = userSettingsProvider.firstDayOfWeek();

        return LocalDate.now()
            .withYear(yearWeek.getYear())
            .with(usersFirstDayOfWeek)
            .with(WEEK_OF_WEEK_BASED_YEAR, weekOfYear);
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
