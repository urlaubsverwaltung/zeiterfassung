package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNullElseGet;

/**
 * Describes an absence like holiday or sick.
 *
 * @param userId
 * @param startDate
 * @param endDate
 * @param dayLength
 * @param type
 * @param color     selected by the user to render the absence type
 */
public record Absence(
    UserId userId,
    ZonedDateTime startDate,
    ZonedDateTime endDate,
    DayLength dayLength,
    AbsenceType type,
    AbsenceColor color
) {

    /**
     *
     * @param locale
     * @return label for the given locale if one exists
     */
    public Optional<String> label(Locale locale) {
        final Map<Locale, String> labelByLocale = requireNonNullElseGet(type.labelByLocale(), HashMap::new);
        return Optional.ofNullable(labelByLocale.get(locale));
    }

    public String getMessageKey() {
        if (this.type.sourceId() == null) {
            return "absence.%s.%s".formatted(type.category(), dayLength.name());
        } else {
            return "absence.%s.%s.%s".formatted(type.category(), this.type.sourceId(), dayLength.name());
        }
    }
}
