package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.AbsenceColor;
import de.focusshift.zeiterfassung.absence.AbsenceType;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimeEntryDayTest {

    @Test
    void ensurePlannedWorkingHours() {

        final TimeEntryDay sut = new TimeEntryDay(null, PlannedWorkingHours.EIGHT, List.of(), List.of());

        final PlannedWorkingHours actual = sut.plannedWorkingHours();
        assertThat(actual).isEqualTo(PlannedWorkingHours.EIGHT);
    }

    @Test
    void ensurePlannedWorkingHoursWithAbsenceFullDay() {

        final Absence absence = new Absence(
            new UserId("123"),
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            DayLength.FULL,
            AbsenceType.SPECIALLEAVE,
            AbsenceColor.VIOLET
        );

        final TimeEntryDay sut = new TimeEntryDay(null, PlannedWorkingHours.EIGHT, List.of(), List.of(absence));

        final PlannedWorkingHours actual = sut.plannedWorkingHours();
        assertThat(actual).isEqualTo(PlannedWorkingHours.ZERO);
    }

    @ParameterizedTest
    @EnumSource(value = DayLength.class, names = { "MORNING", "NOON" })
    void ensurePlannedWorkingHoursWithAbsenceFullDay(DayLength givenDayLength) {

        final Absence absence = new Absence(
            new UserId("123"),
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            givenDayLength,
            AbsenceType.SPECIALLEAVE,
            AbsenceColor.VIOLET
        );

        final TimeEntryDay sut = new TimeEntryDay(null, PlannedWorkingHours.EIGHT, List.of(), List.of(absence));

        final PlannedWorkingHours actual = sut.plannedWorkingHours();
        assertThat(actual).isEqualTo(new PlannedWorkingHours(Duration.ofHours(4)));
    }
}
