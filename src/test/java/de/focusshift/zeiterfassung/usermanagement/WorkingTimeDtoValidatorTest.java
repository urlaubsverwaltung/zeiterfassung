package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.workingtime.WorkingTimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.MapBindingResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.StringUtils.capitalize;

@ExtendWith(MockitoExtension.class)
class WorkingTimeDtoValidatorTest {

    private WorkingTimeDtoValidator sut;

    @Mock
    private WorkingTimeService workingTimeService;

    @BeforeEach
    void setUp() {
        sut = new WorkingTimeDtoValidator(workingTimeService);
    }

    @Test
    void ensureSupports() {
        assertThat(sut.supports(WorkingTimeDto.class)).isTrue();
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
    void invalidWhenValidFromIsNotSet() {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setValidFrom(null);
        dto.setWorkday(List.of());

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.getFieldError("validFrom")).isNotNull();
        assertThat(bindingResult.getFieldError("validFrom").getCode()).isEqualTo("usermanagement.working-time.validation.validFrom.not-empty");
    }

    @Test
    void validWhenWorkdayIsNotSelected() {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setWorkday(List.of());
        dto.setWorkingTime(8.0);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasFieldErrors("workday")).isFalse();
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void validWhenWorkingTimeIsNull(DayOfWeek dayOfWeek) {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setWorkday(List.of(dayOfWeek.name().toLowerCase()));
        dto.setWorkingTime(null);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasFieldErrors("workingTime")).isFalse();
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void validWhenWorkingTimeIsZero(DayOfWeek dayOfWeek) {

        final WorkingTimeDto dto = new WorkingTimeDto();
        dto.setWorkday(List.of(dayOfWeek.name().toLowerCase()));
        dto.setWorkingTime(0.0);

        final MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "");
        sut.validate(dto, bindingResult);

        assertThat(bindingResult.hasFieldErrors("workingTime")).isFalse();
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
