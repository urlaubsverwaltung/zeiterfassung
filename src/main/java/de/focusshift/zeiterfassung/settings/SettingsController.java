package de.focusshift.zeiterfassung.settings;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import static de.focusshift.zeiterfassung.settings.FederalStateSelectDtoFactory.federalStateSelectDto;

@Controller
@RequestMapping("/settings")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL')")
class SettingsController implements HasLaunchpad, HasTimeClock {

    private final SettingsService settingsService;

    SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    String getSettings() {
        return "redirect:settings/federal-state";
    }

    @GetMapping("/federal-state")
    String getFederalStateSettings(Model model) {

        final FederalStateSettings settings = settingsService.getFederalStateSettings();
        final FederalStateSettingsDto federalStateSettingsDto = toFederalStateSettingsDto(settings);

        model.addAttribute("federalStateSettings", federalStateSettingsDto);
        model.addAttribute("federalStateSelect", federalStateSelectDto(federalStateSettingsDto.federalState()));

        return "settings/settings";
    }

    @PostMapping("/federal-state")
    ModelAndView saveSettings(@ModelAttribute("federalStateSettings") FederalStateSettingsDto federalStateSettings, BindingResult result) {

        if (result.hasErrors()) {
            return new ModelAndView("settings/settings");
        }

        settingsService.updateFederalStateSettings(federalStateSettings.federalState(), federalStateSettings.worksOnPublicHoliday());

        return new ModelAndView("redirect:/settings/federal-state");
    }

    private FederalStateSettingsDto toFederalStateSettingsDto(FederalStateSettings federalStateSettings) {
        return new FederalStateSettingsDto(federalStateSettings.federalState(), federalStateSettings.worksOnPublicHoliday());
    }
}
