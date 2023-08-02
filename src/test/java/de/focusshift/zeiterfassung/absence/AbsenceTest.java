package de.focusshift.zeiterfassung.absence;

import org.junit.jupiter.api.Test;

import static de.focusshift.zeiterfassung.absence.AbsenceType.HOLIDAY;
import static de.focusshift.zeiterfassung.absence.AbsenceType.SICK;
import static de.focusshift.zeiterfassung.absence.DayLength.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AbsenceTest {

    @Test
    void getMessageKey() {

        final Absence sick = new Absence(null, null, null, FULL, SICK, null);
        final Absence holiday = new Absence(null, null, null, FULL, HOLIDAY, null);

        assertThat(sick.getMessageKey()).isEqualTo("absence.SICK.FULL");
        assertThat(holiday.getMessageKey()).isEqualTo("absence.HOLIDAY.1000.FULL");
    }
}
