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
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
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
        final WorkingTimeDto.Builder builder = WorkingTimeDto.builder().validFrom(LocalDate.now());
        final Map<DayOfWeek, Consumer<Double>> setterByDayOfWeek = setterByDayOfWeek(builder);

        builder.workday(List.of(dayOfWeek));
        setterByDayOfWeek.get(dayOfWeek).accept(10.0);

        final WorkingTimeDto dto = builder.build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void validWhenWorkingTimeIsSetButNoDayOfWeekHours(DayOfWeek dayOfWeek) {

        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .validFrom(LocalDate.now())
            .workday(List.of(dayOfWeek))
            .workingTime(10.0)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }

    @Test
    void invalidWhenNothingIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder().build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("empty")).isNotNull();
        assertThat(bindingResult.getFieldError("empty").getCode()).isEqualTo("usermanagement.working-time.validation.not-empty");
    }

    @Test
    void invalidWhenValidFromIsNotSetAndIdIsNull() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .id(null)
            .validFrom(null)
            .workday(List.of())
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("validFrom")).isNotNull();
        assertThat(bindingResult.getFieldError("validFrom").getCode()).isEqualTo("usermanagement.working-time.validation.validFrom.not-empty");
    }

    @Test
    void validWhenValidFromIsNotSetButIdIsNotNull() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .id(UUID.randomUUID().toString())
            .validFrom(null)
            .workday(List.of())
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("validFrom")).isNull();
    }

    @Test
    void invalidWhenWorkdayIsNotSelected() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of())
            .workingTime(8.0)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workday")).isNotNull();
        assertThat(bindingResult.getFieldError("workday").getCode()).isEqualTo("usermanagement.working-time.validation.workday.not-empty");
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void invalidWhenWorkingTimeIsNull(DayOfWeek dayOfWeek) {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(dayOfWeek))
            .workingTime(null)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTime")).isNotNull();
        assertThat(bindingResult.getFieldError("workingTime").getCode()).isEqualTo("usermanagement.working-time.validation.working-time.not-empty");
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void invalidWhenWorkingTimeIsZero(DayOfWeek dayOfWeek) {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(dayOfWeek))
            .workingTime(0.0)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTime")).isNotNull();
        assertThat(bindingResult.getFieldError("workingTime").getCode()).isEqualTo("usermanagement.working-time.validation.working-time.not-empty");
    }

    @Test
    void invalidWhenWorkingTimeIsNegative() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workingTime(-1.0)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTime")).isNotNull();
        assertThat(bindingResult.getFieldError("workingTime").getCode()).isEqualTo("usermanagement.working-time.validation.positive-or-zero");
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void invalidWhenDayOfWeekWorkingTimeIsNegative(DayOfWeek dayOfWeek) {

        final WorkingTimeDto.Builder builder = WorkingTimeDto.builder();
        final Map<DayOfWeek, Consumer<Double>> setterByDayOfWeek = setterByDayOfWeek(builder);

        builder.workday(List.of(dayOfWeek));
        setterByDayOfWeek.get(dayOfWeek).accept(-1.0);

        final WorkingTimeDto dto = builder.build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        final String field = "workingTime" + capitalize(dayOfWeek.name().toLowerCase());
        assertThat(bindingResult.getFieldError(field)).isNotNull();
        assertThat(bindingResult.getFieldError(field).getCode()).isEqualTo("usermanagement.working-time.validation.positive-or-zero");
    }

    @Test
    void invalidWhenWorkingTimeIsGreaterThan24() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workingTime(24.1)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTime")).isNotNull();
        assertThat(bindingResult.getFieldError("workingTime").getCode()).isEqualTo("usermanagement.working-time.validation.max");
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void invalidWhenDayOfWeekWorkingTimeIsGreaterThan24(DayOfWeek dayOfWeek) {

        final WorkingTimeDto.Builder builder = WorkingTimeDto.builder();
        final Map<DayOfWeek, Consumer<Double>> setterByDayOfWeek = setterByDayOfWeek(builder);

        builder.workday(List.of(dayOfWeek));
        setterByDayOfWeek.get(dayOfWeek).accept(24.1);

        final WorkingTimeDto dto = builder.build();

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

        final WorkingTimeDto.Builder builder = WorkingTimeDto.builder();
        final Map<DayOfWeek, Consumer<Double>> setterByDayOfWeek = setterByDayOfWeek(builder);

        builder.workday(List.of(otherDayOfWeek));
        setterByDayOfWeek.get(dayOfWeek).accept(8.0);

        final WorkingTimeDto dto = builder.build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        final String name = dayOfWeek.name().toLowerCase();
        final String field = "workingTime" + capitalize(name);
        assertThat(bindingResult.getFieldError(field)).isNotNull();
        assertThat(bindingResult.getFieldError(field).getCode()).isEqualTo("usermanagement.working-time.validation.hours.%s.no-workday".formatted(name));
    }

    private static Map<DayOfWeek, Consumer<Double>> setterByDayOfWeek(WorkingTimeDto.Builder builder) {
        return Map.of(
            MONDAY, builder::workingTimeMonday,
            TUESDAY, builder::workingTimeTuesday,
            WEDNESDAY, builder::workingTimeWednesday,
            THURSDAY, builder::workingTimeThursday,
            FRIDAY, builder::workingTimeFriday,
            SATURDAY, builder::workingTimeSaturday,
            SUNDAY, builder::workingTimeSunday
        );
    }
}
