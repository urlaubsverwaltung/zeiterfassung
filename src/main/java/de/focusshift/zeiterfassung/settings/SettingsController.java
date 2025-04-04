package de.focusshift.zeiterfassung.settings;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import static de.focusshift.zeiterfassung.settings.FederalStateSelectDtoFactory.federalStateSelectDto;
import static java.util.Objects.requireNonNullElse;

@Controller
@RequestMapping("/settings")
@PreAuthorize("hasAnyAuthority('ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL', 'ZEITERFASSUNG_SETTINGS_GLOBAL')")
class SettingsController implements HasLaunchpad, HasTimeClock {

    private static final String ATTRIBUTE_NAME_SETTINGS = "settings";

    private final SettingsService settingsService;

    SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    String getSettings(Model model) {
        fillFederalStateSettings(model);
        return "settings/settings";
    }

    void fillFederalStateSettings(Model model) {

        final FederalStateSettings federalStateSettings = settingsService.getFederalStateSettings();
        final LockTimeEntriesSettings lockTimeEntriesSettings = settingsService.getLockTimeEntriesSettings();
        final SettingsDto settingsDto = toSettingsDto(federalStateSettings, lockTimeEntriesSettings);

        prepareModel(model, settingsDto);
    }

    @PostMapping
    ModelAndView saveSettings(@Valid @ModelAttribute(ATTRIBUTE_NAME_SETTINGS) SettingsDto settingsDto, BindingResult bindingResult, Model model) {

        if (bindingResult.getTarget() == null) {
            // entering a string for lockTimeEntriesDaysInPast results in a TypeMismatchException which skips
            // spring bean validation and therefore there is no binding yet.
            bindingResult = new BeanPropertyBindingResult(settingsDto, ATTRIBUTE_NAME_SETTINGS);
        }

        final int lockTimeEntriesDaysInPast = requireNonNullElse(settingsDto.lockTimeEntriesDaysInPast(), -1);
        if (settingsDto.lockingIsActive() && lockTimeEntriesDaysInPast < 0)  {
            bindingResult.rejectValue( "lockTimeEntriesDaysInPast", "settings.lock-timeentries-days-in-past.validation.positiveOrZero");
        }

        if (bindingResult.hasErrors()) {
            prepareModel(model, settingsDto);
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + ATTRIBUTE_NAME_SETTINGS, bindingResult);
            return new ModelAndView("settings/settings");
        }

        settingsService.updateFederalStateSettings(settingsDto.federalState(), settingsDto.worksOnPublicHoliday());
        settingsService.updateLockTimeEntriesSettings(settingsDto.lockingIsActive(), lockTimeEntriesDaysInPast);

        return new ModelAndView("redirect:/settings");
    }

    private void prepareModel(Model model, SettingsDto settingsDto) {
        model.addAttribute(ATTRIBUTE_NAME_SETTINGS, settingsDto);
        model.addAttribute("federalStateSelect", federalStateSelectDto(settingsDto.federalState()));
    }

    private SettingsDto toSettingsDto(FederalStateSettings federalStateSettings, LockTimeEntriesSettings lockTimeEntriesSettings) {

        final int lockTimeEntriesDaysInPast = lockTimeEntriesSettings.lockTimeEntriesDaysInPast();

        return new SettingsDto(
            federalStateSettings.federalState(),
            federalStateSettings.worksOnPublicHoliday(),
            lockTimeEntriesSettings.lockingIsActive(),
            lockTimeEntriesDaysInPast >= 0 ? lockTimeEntriesDaysInPast : null
        );
    }
}
