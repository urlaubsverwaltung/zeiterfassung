package de.focusshift.zeiterfassung.user;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

@Service
public class DateFormatterImpl implements DateFormatter {

    public String formatYearMonth(YearMonth yearMonth) {
        final DateTimeFormatter formatYearMonth = DateTimeFormatter.ofPattern("MMMM yyyy", locale());
        return formatYearMonth.format(yearMonth);
    }

    public String formatDayOfWeek(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale());
    }

    public String formatDayOfWeekNarrow(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.NARROW_STANDALONE, locale());
    }

    public String formatDayOfWeekShort(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.SHORT_STANDALONE, locale());
    }

    public String formatDayOfWeekFull(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale());
    }

    public String formatDate(LocalDate date) {
        final DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("dd.MM.yyyy", locale());
        return formatDate.format(date);
    }

    public String formatDate(LocalDate date, MonthFormat monthFormat, YearFormat yearFormat) {

        final Locale locale = locale();

        final String day = "dd.";
        final String month = monthFormat == MonthFormat.NONE ? "" : " " + monthFormat.getFormat();
        final String year = yearFormat == YearFormat.NONE ? "" : " " + yearFormat.getFormat();

        final String pattern = day + month + year;

        return DateTimeFormatter.ofPattern(pattern, locale).format(date);
    }

    public String formatYearMonthWeek(LocalDate date) {
        final String yearMonth = formatYearMonth(YearMonth.from(date));
        return yearMonth + " KW " + date.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
    }

    private Locale locale() {
        return LocaleContextHolder.getLocale();
    }
}
