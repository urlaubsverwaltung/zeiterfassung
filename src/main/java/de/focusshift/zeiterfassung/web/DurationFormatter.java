package de.focusshift.zeiterfassung.web;

import org.springframework.context.MessageSource;

import java.time.Duration;
import java.util.Locale;

public class DurationFormatter {

    public static String toDurationString(Duration javaTimeDuration, MessageSource messageSource, Locale locale) {
        if (javaTimeDuration == null) {
            return "";
        }

        final boolean negative = javaTimeDuration.isNegative();
        final long hours = javaTimeDuration.abs().toHours();
        final int minutes = javaTimeDuration.abs().toMinutesPart();

        String value = "";

        if (hours > 0) {
            value += hours + " " + messageSource.getMessage("duration.hours", new Object[]{}, locale);
        }

        if (minutes > 0) {
            if (hours > 0) {
                value += " ";
            }
            value += minutes + " " + messageSource.getMessage("duration.minutes", new Object[]{}, locale);
        }

        return negative ? "-" + value : value;
    }

    private DurationFormatter() {
        // ok
    }
}
