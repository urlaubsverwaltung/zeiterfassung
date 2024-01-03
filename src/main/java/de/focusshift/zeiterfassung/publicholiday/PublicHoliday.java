package de.focusshift.zeiterfassung.publicholiday;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Describes a public holiday for the given date.
 */
public record PublicHoliday(LocalDate date, Function<Locale, String> description) {

    public String description(Locale locale) {
        return description.apply(locale);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicHoliday that = (PublicHoliday) o;
        return Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }
}
