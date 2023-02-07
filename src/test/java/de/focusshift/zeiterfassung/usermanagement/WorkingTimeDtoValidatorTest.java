package de.focusshift.zeiterfassung.usermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.MapBindingResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void ensureErrorWhenNoWorkingTimeIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(MONDAY))
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTimeClash").getCode())
            .isEqualTo("usermanagement.working-time.clash.constraint.message");
    }

    @Test
    void ensureValidWhenZeroWorkingTimeIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(MONDAY))
            .workingTime(BigDecimal.ZERO)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }

    @Test
    void ensureErrorWhenWorkingTimeAndMondayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(MONDAY))
            .workingTime(BigDecimal.TEN)
            .workingTimeMonday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTimeClash").getCode())
            .isEqualTo("usermanagement.working-time.clash.constraint.message");
    }

    @Test
    void ensureValidWhenMondayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(MONDAY))
            .workingTime(BigDecimal.ZERO)
            .workingTimeMonday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }

    @Test
    void ensureErrorWhenWorkingTimeAndTuesdayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(TUESDAY))
            .workingTime(BigDecimal.TEN)
            .workingTimeTuesday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTimeClash").getCode())
            .isEqualTo("usermanagement.working-time.clash.constraint.message");
    }

    @Test
    void ensureValidWhentuesdayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(TUESDAY))
            .workingTime(BigDecimal.ZERO)
            .workingTimeTuesday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }

    @Test
    void ensureErrorWhenWorkingTimeAndWednesdayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(WEDNESDAY))
            .workingTime(BigDecimal.TEN)
            .workingTimeWednesday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTimeClash").getCode())
            .isEqualTo("usermanagement.working-time.clash.constraint.message");
    }

    @Test
    void ensureValidWhenWednesdayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(WEDNESDAY))
            .workingTime(BigDecimal.ZERO)
            .workingTimeWednesday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }

    @Test
    void ensureErrorWhenWorkingTimeAndThursdayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(THURSDAY))
            .workingTime(BigDecimal.TEN)
            .workingTimeThursday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTimeClash").getCode())
            .isEqualTo("usermanagement.working-time.clash.constraint.message");
    }

    @Test
    void ensureValidWhenThursdayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(THURSDAY))
            .workingTime(BigDecimal.ZERO)
            .workingTimeThursday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }

    @Test
    void ensureErrorWhenWorkingTimeAndFridayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(FRIDAY))
            .workingTime(BigDecimal.TEN)
            .workingTimeFriday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTimeClash").getCode())
            .isEqualTo("usermanagement.working-time.clash.constraint.message");
    }

    @Test
    void ensureValidWhenFridayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(FRIDAY))
            .workingTime(BigDecimal.ZERO)
            .workingTimeFriday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }

    @Test
    void ensureErrorWhenWorkingTimeAndSaturdayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(SATURDAY))
            .workingTime(BigDecimal.TEN)
            .workingTimeSaturday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTimeClash").getCode())
            .isEqualTo("usermanagement.working-time.clash.constraint.message");
    }

    @Test
    void ensureValidWhenSaturdayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(SATURDAY))
            .workingTime(BigDecimal.ZERO)
            .workingTimeSaturday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }

    @Test
    void ensureErrorWhenWorkingTimeAndSundayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(SUNDAY))
            .workingTime(BigDecimal.TEN)
            .workingTimeSunday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("workingTimeClash").getCode())
            .isEqualTo("usermanagement.working-time.clash.constraint.message");
    }

    @Test
    void ensureValidWhenSundayIsSet() {
        final WorkingTimeDto dto = WorkingTimeDto.builder()
            .workday(List.of(SUNDAY))
            .workingTime(BigDecimal.ZERO)
            .workingTimeSunday(BigDecimal.TEN)
            .build();

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }
}
