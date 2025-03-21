package de.focusshift.zeiterfassung.usermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.validation.MapBindingResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.StringUtils.capitalize;

class WorkingTimeDtoValidatorTest {

    private WorkingTimeDtoValidator sut;

    @BeforeEach
    void setUp() {
        sut = new WorkingTimeDtoValidator();
    }

    @Test
    void ensureSupports() {
        assertThat(sut.supports(WorkingTimeDto.class)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void validWhenDayOfWeekIsSet(DayOfWeek dayOfWeek) {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setValidFrom(LocalDate.now());
        setOneWorkday(dto, dayOfWeek, 10.0);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void validWhenWorkingTimeIsSetButNoDayOfWeekHours(DayOfWeek dayOfWeek) {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setValidFrom(LocalDate.now());
        setOneWorkday(dto, dayOfWeek, 10.0);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }

    @Test
    void invalidWhenNothingIsSet() {

        final WorkingTimeDto dto = new WorkingTimeDto();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("empty")).isNotNull();
        assertThat(bindingResult.getFieldError("empty").getCode()).isEqualTo("usermanagement.working-time.validation.not-empty");
    }

    @Test
    void invalidWhenValidFromIsNotSetAndIdIsNull() {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setId(null);
        dto.setValidFrom(null);
        dto.setWorkday(List.of());

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("validFrom")).isNotNull();
        assertThat(bindingResult.getFieldError("validFrom").getCode()).isEqualTo("usermanagement.working-time.validation.validFrom.not-empty");
    }

    @Test
    void validWhenValidFromIsNotSetButIdIsNotNull() {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setValidFrom(null);
        dto.setWorkday(List.of());

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("validFrom")).isNull();
    }

    @Test
    void invalidWhenWorkdayIsNotSelected() {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setWorkday(List.of());
        dto.setWorkingTime(8.0);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workday")).isNotNull();
        assertThat(bindingResult.getFieldError("workday").getCode()).isEqualTo("usermanagement.working-time.validation.workday.not-empty");
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void invalidWhenWorkingTimeIsNull(DayOfWeek dayOfWeek) {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setWorkday(List.of(dayOfWeek.name().toLowerCase()));
        dto.setWorkingTime(null);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTime")).isNotNull();
        assertThat(bindingResult.getFieldError("workingTime").getCode()).isEqualTo("usermanagement.working-time.validation.working-time.not-empty");
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void validWhenWorkingTimeIsZero(DayOfWeek dayOfWeek) {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setWorkday(List.of(dayOfWeek.name().toLowerCase()));
        dto.setWorkingTime(0.0);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTime")).isNull();
    }

    @Test
    void invalidWhenWorkingTimeIsNegative() {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setWorkingTime(-1.0);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTime")).isNotNull();
        assertThat(bindingResult.getFieldError("workingTime").getCode()).isEqualTo("usermanagement.working-time.validation.positive-or-zero");
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void invalidWhenDayOfWeekWorkingTimeIsNegative(DayOfWeek dayOfWeek) {

        final WorkingTimeDto dto = new WorkingTimeDto();
        setOneWorkday(dto, dayOfWeek, -1.0);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        final String field = "workingTime" + capitalize(dayOfWeek.name().toLowerCase());
        assertThat(bindingResult.getFieldError(field)).isNotNull();
        assertThat(bindingResult.getFieldError(field).getCode()).isEqualTo("usermanagement.working-time.validation.positive-or-zero");
    }

    @Test
    void invalidWhenWorkingTimeIsGreaterThan24() {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setWorkingTime(24.1);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTime")).isNotNull();
        assertThat(bindingResult.getFieldError("workingTime").getCode()).isEqualTo("usermanagement.working-time.validation.max");
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void invalidWhenDayOfWeekWorkingTimeIsGreaterThan24(DayOfWeek dayOfWeek) {

        final WorkingTimeDto dto = new WorkingTimeDto();
        setOneWorkday(dto, dayOfWeek, 24.1);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        final String field = "workingTime" + capitalize(dayOfWeek.name().toLowerCase());
        assertThat(bindingResult.getFieldError(field)).isNotNull();
        assertThat(bindingResult.getFieldError(field).getCode()).isEqualTo("usermanagement.working-time.validation.max");
    }

    private static Stream<Arguments> factory() {
        return Stream.of(
            Arguments.arguments(MONDAY, SUNDAY),
            Arguments.arguments(TUESDAY, SUNDAY),
            Arguments.arguments(WEDNESDAY, SUNDAY),
            Arguments.arguments(THURSDAY, SUNDAY),
            Arguments.arguments(FRIDAY, SUNDAY),
            Arguments.arguments(SATURDAY, SUNDAY),
            Arguments.arguments(SUNDAY, MONDAY)
        );
    }

    @ParameterizedTest
    @MethodSource("factory")
    void invalidWhenDayOfWeekHoursAreGivenButDayIsNotSelected(DayOfWeek dayOfWeek, DayOfWeek otherDayOfWeek) {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setWorkday(List.of(otherDayOfWeek.name()));
        setWorkingTimeOfDayOfWeek(dto, dayOfWeek, 8.0);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        final String name = dayOfWeek.name().toLowerCase();
        final String field = "workingTime" + capitalize(name);
        assertThat(bindingResult.getFieldError(field)).isNotNull();
        assertThat(bindingResult.getFieldError(field).getCode()).isEqualTo("usermanagement.working-time.validation.hours.%s.no-workday".formatted(name));
    }

    private void setOneWorkday(WorkingTimeDto dto, DayOfWeek dayOfWeek, Double workingTime) {
        dto.setWorkday(List.of(dayOfWeek.name().toLowerCase()));
        setWorkingTimeOfDayOfWeek(dto, dayOfWeek, workingTime);
    }

    private void setWorkingTimeOfDayOfWeek(WorkingTimeDto dto, DayOfWeek dayOfWeek, Double workingTime) {
        switch (dayOfWeek) {
            case MONDAY -> dto.setWorkingTimeMonday(workingTime);
            case TUESDAY -> dto.setWorkingTimeTuesday(workingTime);
            case WEDNESDAY -> dto.setWorkingTimeWednesday(workingTime);
            case THURSDAY -> dto.setWorkingTimeThursday(workingTime);
            case FRIDAY -> dto.setWorkingTimeFriday(workingTime);
            case SATURDAY -> dto.setWorkingTimeSaturday(workingTime);
            case SUNDAY -> dto.setWorkingTimeSunday(workingTime);
        }
    }
}
