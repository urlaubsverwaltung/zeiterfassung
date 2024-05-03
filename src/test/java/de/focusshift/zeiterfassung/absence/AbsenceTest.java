package de.focusshift.zeiterfassung.absence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;
import java.util.Map;

import static de.focusshift.zeiterfassung.absence.AbsenceColor.VIOLET;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.SICK;
import static de.focusshift.zeiterfassung.absence.DayLength.FULL;
import static org.assertj.core.api.Assertions.assertThat;

class AbsenceTest {

    @ParameterizedTest
    @EnumSource(value = AbsenceTypeCategory.class)
    void ensureGetMessageKeyWithCategory(AbsenceTypeCategory givenCategory) {
        final AbsenceType absenceType = new AbsenceType(givenCategory, 100L, Map.of(), VIOLET);
        final Absence sut = new Absence(null, null, null, FULL, absenceType);
        assertThat(sut.getMessageKey()).isEqualTo("absence.%s.100.FULL".formatted(givenCategory.name()));
    }

    @ParameterizedTest
    @EnumSource(value = DayLength.class)
    void ensureGetMessageKeyWithDayLength(DayLength givenDayLength) {
        final AbsenceType absenceType = new AbsenceType(HOLIDAY, 100L, Map.of(), VIOLET);
        final Absence sut = new Absence(null, null, null, givenDayLength, absenceType);
        assertThat(sut.getMessageKey()).isEqualTo("absence.HOLIDAY.100.%s".formatted(givenDayLength.name()));
    }

    @ParameterizedTest
    @ValueSource(longs = {100L, 200L})
    void ensureGetMessageKeyWithSourceId(Long givenSourceId) {
        final AbsenceType absenceType = new AbsenceType(HOLIDAY, givenSourceId, Map.of(), VIOLET);
        final Absence sut = new Absence(null, null, null, FULL, absenceType);
        assertThat(sut.getMessageKey()).isEqualTo("absence.HOLIDAY.%s.FULL".formatted(givenSourceId));
    }

    @Test
    void ensureGetMessageKeyWithSourceIdNull() {
        final AbsenceType absenceType = new AbsenceType(SICK, null, Map.of(), VIOLET);
        final Absence sut = new Absence(null, null, null, FULL, absenceType);
        assertThat(sut.getMessageKey()).isEqualTo("absence.SICK.FULL");
    }

    @Test
    void ensureLabelByLocaleWhenMapIsNull() {
        final AbsenceType absenceType = new AbsenceType(HOLIDAY, 1L, null, VIOLET);
        final Absence sut = new Absence(null, null, null, FULL, absenceType);
        assertThat(sut.label(Locale.GERMAN)).isEmpty();
    }

    @Test
    void ensureLabelByLocaleWhenLocaleDoesNotExist() {
        final AbsenceType absenceType = new AbsenceType(HOLIDAY, 1L, Map.of(), VIOLET);
        final Absence sut = new Absence(null, null, null, FULL, absenceType);
        assertThat(sut.label(Locale.GERMAN)).isEmpty();
    }

    @Test
    void ensureLabelByLocale() {
        final AbsenceType absenceType = new AbsenceType(HOLIDAY, 1L, Map.of(Locale.GERMAN, "familientag"), VIOLET);
        final Absence sut = new Absence(null, null, null, FULL, absenceType);
        assertThat(sut.label(Locale.GERMAN)).hasValue("familientag");
    }
}
