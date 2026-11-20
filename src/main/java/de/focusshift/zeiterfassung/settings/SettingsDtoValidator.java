package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static java.lang.Boolean.TRUE;

@Component
class SettingsDtoValidator implements Validator {

    private final Clock clock;
    private final AutomaticBreakDeductionSettingsService automaticBreakDeductionSettingsService;
    private final UserSettingsProvider userSettingsProvider;

    SettingsDtoValidator(
        Clock clock,
        AutomaticBreakDeductionSettingsService automaticBreakDeductionSettingsService,
        UserSettingsProvider userSettingsProvider
    ) {
        this.clock = clock;
        this.automaticBreakDeductionSettingsService = automaticBreakDeductionSettingsService;
        this.userSettingsProvider = userSettingsProvider;
    }

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
        validateAutomaticBreakDeduction(settingsDto, errors);
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

    private void validateAutomaticBreakDeduction(SettingsDto settingsDto, Errors errors) {
        if (!TRUE.equals(settingsDto.automaticBreakDeductionIsActive())) {
            return;
        }

        final LocalDate date = settingsDto.automaticBreakDeductionActiveDate();
        if (date == null) {
            errors.rejectValue("automaticBreakDeductionActiveDate", "settings.automatic-break-deduction.date.validation.NotNull");
            return;
        }

        // the deduction must not be applied retroactively.
        // an already active deduction keeps its date, otherwise the settings could not be saved anymore.
        final ZoneId zoneId = userSettingsProvider.zoneId();
        final LocalDate today = LocalDate.now(clock.withZone(zoneId));
        if (date.isBefore(today) && !date.equals(activeDeductionDate(zoneId).orElse(null))) {
            errors.rejectValue("automaticBreakDeductionActiveDate", "settings.automatic-break-deduction.date.validation.past");
        }
    }

    private Optional<LocalDate> activeDeductionDate(ZoneId zoneId) {
        return automaticBreakDeductionSettingsService.getAutomaticBreakDeductionSettings()
            .filter(AutomaticBreakDeductionSettings::active)
            .flatMap(settings -> settings.enabledDate(zoneId));
    }
}
