package de.focusshift.zeiterfassung.user;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;

@Component
public class DateRangeFormatter {

    private final DateFormatter dateFormatter;
    private final MessageSource messageSource;

    DateRangeFormatter(DateFormatter dateFormatter, MessageSource messageSource) {
        this.dateFormatter = dateFormatter;
        this.messageSource = messageSource;
    }

    public String toDateRangeString(LocalDate from, LocalDate to) {

        final MonthFormat firstMonthFormat =
            from.getMonthValue() == to.getMonthValue() ? MonthFormat.NONE : MonthFormat.STRING;

        final YearFormat firstYearFormat =
            from.getYear() == to.getYear() ? YearFormat.NONE : YearFormat.FULL;

        final String firstDateString = dateFormatter.formatDate(from, firstMonthFormat, firstYearFormat);
        final String lastDateString = dateFormatter.formatDate(to, MonthFormat.STRING, YearFormat.FULL);

        return messageSource.getMessage("date-range", new Object[]{firstDateString, lastDateString}, locale());
    }

    private Locale locale() {
        return LocaleContextHolder.getLocale();
    }
}
