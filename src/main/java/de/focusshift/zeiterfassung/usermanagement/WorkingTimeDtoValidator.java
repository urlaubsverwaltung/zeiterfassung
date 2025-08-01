package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.workingtime.WorkingTime;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeId;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeService;
import org.slf4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.StringUtils.capitalize;

@Component
class WorkingTimeDtoValidator implements Validator {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final BigDecimal MAX_HOURS = BigDecimal.valueOf(24);

    private final WorkingTimeService workingTimeService;

    WorkingTimeDtoValidator(WorkingTimeService workingTimeService) {
        this.workingTimeService = workingTimeService;
    }

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return WorkingTimeDto.class.equals(clazz);
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {

        final WorkingTimeDto dto = (WorkingTimeDto) target;

        if ((dto.getId() == null || !isLegacyDefaultWorkingTime(dto)) && dto.getValidFrom() == null) {
            errors.rejectValue("validFrom", "usermanagement.working-time.validation.validFrom.not-empty");
        }

        validateDays(dto, WorkingTimeDtoValidator::positiveOrZero,
            field -> errors.rejectValue(field, "usermanagement.working-time.validation.positive-or-zero")
        );

        validateDays(dto, WorkingTimeDtoValidator::max,
            field -> errors.rejectValue(field, "usermanagement.working-time.validation.max")
        );
    }

    private boolean isLegacyDefaultWorkingTime(WorkingTimeDto dto) {

        final List<WorkingTime> allWorkingTimes = workingTimeService.getAllWorkingTimesByUser(new UserLocalId(dto.getUserId()));
        final WorkingTime oldestWorkingTime = allWorkingTimes.getLast();

        UUID dtoId;
        try {
            dtoId = UUID.fromString(dto.getId());
        } catch (IllegalArgumentException exception) {
            LOG.warn("received workingTime id could not be parsed to UUID. This should not happen when the user clicks through the UI.", exception);
            return false;
        }

        return oldestWorkingTime.id().equals(new WorkingTimeId(dtoId));
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
}
