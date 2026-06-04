package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.activitytype.ActivityTypeService;
import de.focusshift.zeiterfassung.project.ProjectService;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDate;

import static java.lang.Boolean.TRUE;

@Component
class SettingsDtoValidator implements Validator {

    private final ProjectService projectService;
    private final ActivityTypeService activityTypeService;

    SettingsDtoValidator(ProjectService projectService, ActivityTypeService activityTypeService) {
        this.projectService = projectService;
        this.activityTypeService = activityTypeService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return SettingsDto.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        final SettingsDto settingsDto = (SettingsDto) target;
        validateFederalState(settingsDto, errors);
        validateWorkingTime(settingsDto, errors);
        validateTimeRoundingMinutes(settingsDto, errors);
        validateMinSuggestedMinutes(settingsDto, errors);
        validateLockTimeEntriesDaysInPast(settingsDto, errors);
        validateSubtractBreak(settingsDto, errors);
        validateProjectRequired(settingsDto, errors);
        validateActivityTypeRequired(settingsDto, errors);
    }

    private void validateWorkingTime(SettingsDto settingsDto, Errors errors) {
        final Double hours = settingsDto.workingTime();
        if (hours != null && (hours < 0 || hours > 24)) {
            errors.rejectValue("workingTime", "settings.working-time.validation.range");
        }
    }

    private void validateTimeRoundingMinutes(SettingsDto settingsDto, Errors errors) {
        final Integer rounding = settingsDto.timeRoundingMinutes();
        if (rounding != null && (rounding < 1 || rounding > 60)) {
            errors.rejectValue("timeRoundingMinutes", "settings.time-rounding-minutes.validation.range");
        }
    }

    private void validateMinSuggestedMinutes(SettingsDto settingsDto, Errors errors) {
        final Integer min = settingsDto.minSuggestedMinutes();
        if (min != null && (min < 1 || min > 480)) {
            errors.rejectValue("minSuggestedMinutes", "settings.min-suggested-minutes.validation.range");
        }
    }

    private void validateFederalState(SettingsDto settingsDto, Errors errors) {
        if (settingsDto.federalState() == null) {
            errors.rejectValue("federalState", "jakarta.validation.constraints.NotNull.message");
        }
    }

    private void validateLockTimeEntriesDaysInPast(SettingsDto settingsDto, Errors errors) {
        if (settingsDto.lockingIsActive()) {
            final Integer lockTimeEntriesDaysInPast = settingsDto.lockTimeEntriesDaysInPastAsNumber();
            if (lockTimeEntriesDaysInPast == null || lockTimeEntriesDaysInPast < 0) {
                errors.rejectValue("lockTimeEntriesDaysInPast", "settings.lock-timeentries-days-in-past.validation.positiveOrZero");
            }
        }
    }

    private void validateSubtractBreak(SettingsDto settingsDto, Errors errors) {
        // can be: null, false or true
        if (TRUE.equals(settingsDto.subtractBreakFromTimeEntryIsActive())) {
            final LocalDate date = settingsDto.subtractBreakFromTimeEntryActiveDate();
            if (date == null) {
                errors.rejectValue("subtractBreakFromTimeEntryActiveDate", "settings.work-duration.calculation.subtract-breaks.date.validation.NotNull");
            }
        }
    }

    private void validateProjectRequired(SettingsDto settingsDto, Errors errors) {
        if (TRUE.equals(settingsDto.projectRequired()) && projectService.findAllActive().isEmpty()) {
            errors.rejectValue("projectRequired", "settings.categorisation.project-required.validation.no-active-projects");
        }
    }

    private void validateActivityTypeRequired(SettingsDto settingsDto, Errors errors) {
        if (TRUE.equals(settingsDto.activityTypeRequired()) && activityTypeService.findAllActive().isEmpty()) {
            errors.rejectValue("activityTypeRequired", "settings.categorisation.activity-type-required.validation.no-active-activity-types");
        }
    }
}
