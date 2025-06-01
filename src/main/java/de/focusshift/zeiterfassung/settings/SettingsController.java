package de.focusshift.zeiterfassung.settings;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import static de.focusshift.zeiterfassung.settings.FederalStateSelectDtoFactory.federalStateSelectDto;
import static java.util.Objects.requireNonNullElse;

@Controller
@RequestMapping("/settings")
@PreAuthorize("hasAnyAuthority('ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL', 'ZEITERFASSUNG_SETTINGS_GLOBAL')")
class SettingsController implements HasLaunchpad, HasTimeClock {

    private static final String ATTRIBUTE_NAME_SETTINGS = "settings";

    private final SettingsService settingsService;
    private final SettingsDtoValidator settingsDtoValidator;
    private final UserSettingsProvider userSettingsProvider;
    private final Clock clock;

    SettingsController(
        SettingsService settingsService,
        SettingsDtoValidator settingsDtoValidator,
        UserSettingsProvider userSettingsProvider,
        Clock clock
    ) {
        this.settingsService = settingsService;
        this.settingsDtoValidator = settingsDtoValidator;
        this.userSettingsProvider = userSettingsProvider;
        this.clock = clock;
    }

    @GetMapping
    String getSettings(Model model, Locale locale) {

        // settings dto could exist already from POST redirect
        if (!model.containsAttribute(ATTRIBUTE_NAME_SETTINGS)) {
            fillFederalStateSettings(model);
        }

        final SettingsDto settingsDto = getSettingsDtoFromModel(model);
        model.addAttribute("timeslotLockedExampleDate", getTimeslotLockedExampleDate(settingsDto, locale));

        return "settings/settings";
    }

    void fillFederalStateSettings(Model model) {

        final FederalStateSettings federalStateSettings = settingsService.getFederalStateSettings();
        final LockTimeEntriesSettings lockTimeEntriesSettings = settingsService.getLockTimeEntriesSettings();
        final SettingsDto settingsDto = toSettingsDto(federalStateSettings, lockTimeEntriesSettings);

        prepareModel(model, settingsDto);
    }

    @PostMapping
    ModelAndView saveSettings(
        @ModelAttribute(ATTRIBUTE_NAME_SETTINGS) SettingsDto settingsDto,
        BindingResult bindingResult,
        @RequestParam(name = "preview", required = false) Optional<String> preview,
        RedirectAttributes redirectAttributes
    ) {

        settingsDtoValidator.validate(settingsDto, bindingResult);

        if (bindingResult.hasErrors() || preview.isPresent()) {
            redirectAttributes.addFlashAttribute(ATTRIBUTE_NAME_SETTINGS, settingsDto);
            redirectAttributes.addFlashAttribute("federalStateSelect", federalStateSelectDto(settingsDto.federalState()));
            redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + ATTRIBUTE_NAME_SETTINGS, bindingResult);
        } else {
            settingsService.updateFederalStateSettings(settingsDto.federalState(), settingsDto.worksOnPublicHoliday());
            final int lockTimeEntriesDaysInPast = requireNonNullElse(settingsDto.lockTimeEntriesDaysInPastAsNumber(), -1);
            settingsService.updateLockTimeEntriesSettings(settingsDto.lockingIsActive(), lockTimeEntriesDaysInPast);
        }

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
            lockTimeEntriesDaysInPast > -1 ? String.valueOf(lockTimeEntriesDaysInPast) : null
        );
    }

    private SettingsDto getSettingsDtoFromModel(Model model) {
        final SettingsDto settingsDto = (SettingsDto) model.getAttribute(ATTRIBUTE_NAME_SETTINGS);
        if (settingsDto == null) {
            throw new IllegalStateException("expected settingsDto to exist in model.");
        }
        return settingsDto;
    }

    private String getTimeslotLockedExampleDate(SettingsDto settingsDto, Locale locale) {

        final int lockedDaysInPast = requireNonNullElse(settingsDto.lockTimeEntriesDaysInPastAsNumber(), -1);

        final ZoneId userZoneId = userSettingsProvider.zoneId();
        final LocalDate today = LocalDate.now(clock.withZone(userZoneId));
        return today.minusDays(lockedDaysInPast + 1).format(DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", locale));
    }
}
