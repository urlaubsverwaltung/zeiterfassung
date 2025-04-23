package de.focusshift.zeiterfassung.settings;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

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
                errors.rejectValue( "lockTimeEntriesDaysInPast", "settings.lock-timeentries-days-in-past.validation.positiveOrZero");
            }
        }
    }
}
