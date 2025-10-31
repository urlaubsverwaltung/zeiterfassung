package de.focusshift.zeiterfassung.settings;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDate;

import static java.lang.Boolean.TRUE;

@Component
class SettingsDtoValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return SettingsDto.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        final SettingsDto settingsDto = (SettingsDto) target;
        validateFederalState(settingsDto, errors);
        validateLockTimeEntriesDaysInPast(settingsDto, errors);
        validateSubtractBreak(settingsDto, errors);
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
}
