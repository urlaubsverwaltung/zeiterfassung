package de.focusshift.zeiterfassung.usermanagement;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.springframework.util.StringUtils.capitalize;

@Component
class WorkingTimeDtoValidator implements Validator {

    private static final BigDecimal MAX_HOURS = BigDecimal.valueOf(24);

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return WorkingTimeDto.class.equals(clazz);
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {

        final WorkingTimeDto dto = (WorkingTimeDto) target;

        if (dto.getId() == null && dto.getValidFrom() == null) {
            errors.rejectValue("validFrom", "usermanagement.working-time.validation.validFrom.not-empty");
        }

        validateDays(dto, WorkingTimeDtoValidator::positiveOrZero,
            field -> errors.rejectValue(field, "usermanagement.working-time.validation.positive-or-zero")
        );

        validateDays(dto, WorkingTimeDtoValidator::max,
            field -> errors.rejectValue(field, "usermanagement.working-time.validation.max")
        );

        if (noInput(dto)) {
            errors.rejectValue("empty", "usermanagement.working-time.validation.not-empty");
            return;
        }

        // No working day selected
        if (dto.getWorkday().isEmpty()) {
            errors.rejectValue("workday", "usermanagement.working-time.validation.workday.not-empty");
            return;
        }

        // no working hours
        if (zeroOrNull(dto.getWorkingTime())) {
            final List<String> selectedDaysWithoutHours = getSelectedDaysWithoutHours(dto);
            if (selectedDaysWithoutHours.size() == dto.getWorkday().size()) {
                // and nothing else is given
                errors.rejectValue("workingTime", "usermanagement.working-time.validation.working-time.not-empty");
            } else {
                // and at least one selected day has no hours
                for (String day : selectedDaysWithoutHours) {
                    final String field = "workDay" + capitalize(day);
                    errors.rejectValue(field, "usermanagement.working-time.validation.day.%s.no-hours".formatted(day));
                }
            }
        }

        // individual day hours given but day is not selected as working day
        final List<String> daysWithHoursButNotSelected = getDaysWithHoursButNotSelected(dto);
        if (!daysWithHoursButNotSelected.isEmpty()) {
            for (String day : daysWithHoursButNotSelected) {
                final String field = "workingTime" + capitalize(day);
                errors.rejectValue(field, "usermanagement.working-time.validation.hours.%s.no-workday".formatted(day));
            }
        }
    }

    private static boolean positiveOrZero(Double doubleValue) {
        return doubleValue != null && decimal(doubleValue).signum() == -1;
    }

    private static boolean max(Double doubleValue) {
        return doubleValue != null && decimal(doubleValue).compareTo(MAX_HOURS) > 0;
    }

    private static BigDecimal decimal(Double doubleValue) {
        return BigDecimal.valueOf(doubleValue);
    }

    private static void validateDays(WorkingTimeDto dto, Predicate<Double> validator, Consumer<String> reject) {

        if (validator.test(dto.getWorkingTime())) {
            reject.accept("workingTime");
        }

        final Map<String, Supplier<Double>> getter = getHoursSupplierByDayMap(dto);
        for (Map.Entry<String, Supplier<Double>> entry : getter.entrySet()) {
            if (validator.test(entry.getValue().get())) {
                reject.accept("workingTime" + capitalize(entry.getKey()));
            }
        }
    }

    private static List<String> getDaysWithHoursButNotSelected(WorkingTimeDto dto) {
        final List<String> workday = dto.getWorkday();
        final Map<String, Supplier<Double>> hoursByDay = getHoursSupplierByDayMap(dto);

        return hoursByDay.keySet()
            .stream()
            .filter(day -> !zeroOrNull(hoursByDay.get(day).get()) && !workday.contains(day))
            .toList();
    }

    private static List<String> getSelectedDaysWithoutHours(WorkingTimeDto dto) {
        final List<String> workday = dto.getWorkday();
        final Map<String, Supplier<Double>> hoursByDay = getHoursSupplierByDayMap(dto);

        return hoursByDay.keySet()
            .stream()
            .filter(day -> workday.contains(day) && zeroOrNull(hoursByDay.get(day).get()))
            .toList();
    }

    private static Map<String, Supplier<Double>> getHoursSupplierByDayMap(WorkingTimeDto dto) {
        return Map.of(
            "monday", dto::getWorkingTimeMonday,
            "tuesday", dto::getWorkingTimeTuesday,
            "wednesday", dto::getWorkingTimeWednesday,
            "thursday", dto::getWorkingTimeThursday,
            "friday", dto::getWorkingTimeFriday,
            "saturday", dto::getWorkingTimeSaturday,
            "sunday", dto::getWorkingTimeSunday
        );
    }

    private static boolean noInput(WorkingTimeDto dto) {
        return dto.getWorkday().isEmpty()
            && zeroOrNull(dto.getWorkingTime())
            && zeroOrNull(dto.getWorkingTimeMonday())
            && zeroOrNull(dto.getWorkingTimeTuesday())
            && zeroOrNull(dto.getWorkingTimeWednesday())
            && zeroOrNull(dto.getWorkingTimeThursday())
            && zeroOrNull(dto.getWorkingTimeFriday())
            && zeroOrNull(dto.getWorkingTimeSaturday())
            && zeroOrNull(dto.getWorkingTimeSunday());
    }

    private static boolean zeroOrNull(Double doubleValue) {
        return doubleValue == null || doubleValue == 0;
    }
}
