package de.focusshift.zeiterfassung.usermanagement;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

@Component
class WorkingTimeDtoValidator implements Validator {

    private static final String ERROR_WORKING_TIME_CLASH = "usermanagement.working-time.clash.constraint.message";

    @Override
    public boolean supports(Class<?> clazz) {
        return WorkingTimeDto.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        final WorkingTimeDto dto = (WorkingTimeDto) target;

        if (hasSpecificWorkingTimes(dto)) {
            if (dto.getWorkingTime() != null && dto.getWorkingTime().intValue() != 0) {
                errors.rejectValue("workingTimeClash" , ERROR_WORKING_TIME_CLASH);
            }
        } else {
            if (dto.getWorkingTime() == null) {
                errors.rejectValue("workingTimeClash" , ERROR_WORKING_TIME_CLASH);
            }
        }
    }

    private static boolean hasSpecificWorkingTimes(WorkingTimeDto dto) {
        return Stream.of(
            dto.getWorkingTimeMonday(),
            dto.getWorkingTimeTuesday(),
            dto.getWorkingTimeWednesday(),
            dto.getWorkingTimeThursday(),
            dto.getWorkingTimeFriday(),
            dto.getWorkingTimeSaturday(),
            dto.getWorkingTimeSunday()
        ).anyMatch(is(Objects::nonNull).and(not(BigDecimal.ZERO::equals)));
    }

    private static <T> Predicate<T> is(Predicate<T> predicate) {
        return predicate;
    }
}
