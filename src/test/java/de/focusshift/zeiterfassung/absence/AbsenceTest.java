package de.focusshift.zeiterfassung.absence;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.SICK;
import static de.focusshift.zeiterfassung.absence.DayLength.FULL;
import static org.assertj.core.api.Assertions.assertThat;

class AbsenceTest {

    @ParameterizedTest
    @EnumSource(value = AbsenceTypeCategory.class)
    void ensureGetMessageKeyWithCategory(AbsenceTypeCategory givenCategory) {
        final AbsenceType absenceType = new AbsenceType(givenCategory, 100L);
        final Absence sut = new Absence(null, null, null, FULL, absenceType, null);
        assertThat(sut.getMessageKey()).isEqualTo("absence.%s.100.FULL".formatted(givenCategory.name()));
    }

    @ParameterizedTest
    @EnumSource(value = DayLength.class)
    void ensureGetMessageKeyWithDayLength(DayLength givenDayLength) {
        final AbsenceType absenceType = new AbsenceType(HOLIDAY, 100L);
        final Absence sut = new Absence(null, null, null, givenDayLength, absenceType, null);
        assertThat(sut.getMessageKey()).isEqualTo("absence.HOLIDAY.100.%s".formatted(givenDayLength.name()));
    }

    @ParameterizedTest
    @ValueSource(longs = {100L, 200L})
    void ensureGetMessageKeyWithSourceId(Long givenSourceId) {
        final AbsenceType absenceType = new AbsenceType(HOLIDAY, givenSourceId);
        final Absence sut = new Absence(null, null, null, FULL, absenceType, null);
        assertThat(sut.getMessageKey()).isEqualTo("absence.HOLIDAY.%s.FULL".formatted(givenSourceId));
    }

    @Test
    void ensureGetMessageKeyWithSourceIdNull() {
        final AbsenceType absenceType = new AbsenceType(SICK, null);
        final Absence sut = new Absence(null, null, null, FULL, absenceType, null);
        assertThat(sut.getMessageKey()).isEqualTo("absence.HOLIDAY.FULL");
    }
}
