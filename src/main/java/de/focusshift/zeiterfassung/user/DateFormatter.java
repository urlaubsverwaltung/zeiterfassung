package de.focusshift.zeiterfassung.user;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

public interface DateFormatter {

    String formatYearMonth(YearMonth yearMonth);

    String formatDayOfWeek(DayOfWeek dayOfWeek);

    String formatDayOfWeekNarrow(DayOfWeek dayOfWeek);

    String formatDayOfWeekShort(DayOfWeek dayOfWeek);

    String formatDayOfWeekFull(DayOfWeek dayOfWeek);

    String formatDate(LocalDate date);

    String formatDate(LocalDate date, MonthFormat monthFormat, YearFormat yearFormat);

    String formatYearMonthWeek(LocalDate date);
}
