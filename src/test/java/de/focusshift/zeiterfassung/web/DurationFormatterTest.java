package de.focusshift.zeiterfassung.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.Duration;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DurationFormatterTest {

    public static final Locale GERMAN = Locale.GERMAN;

    @Mock
    private MessageSource messageSource;

    @Test
    void ensuresDurationStringWithHoursAndMinutes() {

        when(messageSource.getMessage("duration.hours", new Object[]{23L}, GERMAN)).thenReturn("23 Stunden");
        when(messageSource.getMessage("duration.minutes", new Object[]{32}, GERMAN)).thenReturn("32 Minuten");

        final Duration duration = Duration.ofMinutes(23*60+32);
        final String durationString = DurationFormatter.toDurationString(duration, messageSource, GERMAN);
        assertThat(durationString).isEqualTo("23 Stunden 32 Minuten");
    }

    @Test
    void ensuresDurationStringWithMoreThanOneDayWithMinutes() {

        when(messageSource.getMessage("duration.hours", new Object[]{51L}, GERMAN)).thenReturn("51 Stunden");
        when(messageSource.getMessage("duration.minutes", new Object[]{1}, GERMAN)).thenReturn("1 Minute");

        final Duration duration = Duration.ofDays(2).plus(Duration.ofHours(2)).plus(Duration.ofMinutes(61));
        final String durationString = DurationFormatter.toDurationString(duration, messageSource, GERMAN);
        assertThat(durationString).isEqualTo("51 Stunden 1 Minute");
    }

    @Test
    void ensuresDurationStringWithOnlyMinutes() {

        when(messageSource.getMessage("duration.minutes", new Object[]{1}, GERMAN)).thenReturn("1 Minute");

        final Duration duration = Duration.ofMinutes(1);
        final String durationString = DurationFormatter.toDurationString(duration, messageSource, GERMAN);
        assertThat(durationString).isEqualTo("1 Minute");
    }

    @Test
    void ensuresDurationStringIsNegative() {

        when(messageSource.getMessage("duration.hours", new Object[]{1L}, GERMAN)).thenReturn("1 Stunde");
        when(messageSource.getMessage("duration.minutes", new Object[]{15}, GERMAN)).thenReturn("15 Minuten");

        final Duration duration = Duration.ofMinutes(60+15).negated();
        final String durationString = DurationFormatter.toDurationString(duration, messageSource, GERMAN);
        assertThat(durationString).isEqualTo("-1 Stunde 15 Minuten");
    }

    @Test
    void ensuresDurationStringIsNegativeForHours() {

        when(messageSource.getMessage("duration.hours", new Object[]{24L}, GERMAN)).thenReturn("24 Stunden");

        final Duration duration = Duration.ofDays(-1);
        final String durationString = DurationFormatter.toDurationString(duration, messageSource, GERMAN);
        assertThat(durationString).isEqualTo("-24 Stunden");
    }

    @Test
    void ensuresDurationStringIsNegativeForMinutes() {

        when(messageSource.getMessage("duration.minutes", new Object[]{24}, GERMAN)).thenReturn("24 Minuten");

        final Duration duration = Duration.ofMinutes(-24);
        final String durationString = DurationFormatter.toDurationString(duration, messageSource, GERMAN);
        assertThat(durationString).isEqualTo("-24 Minuten");
    }

    @Test
    void ensuresEmptyStringOnNull() {
        final String durationString = DurationFormatter.toDurationString(null, messageSource, GERMAN);
        assertThat(durationString).isEmpty();
    }

    @Test
    void ensuresWithEmptyHoursAndMinutes() {
        final String durationString = DurationFormatter.toDurationString(Duration.ofSeconds(0), messageSource, GERMAN);
        assertThat(durationString).isEmpty();
    }
}
