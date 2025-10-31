package de.focusshift.zeiterfassung.settings;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import jakarta.annotation.Nullable;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import static de.focusshift.zeiterfassung.settings.FederalStateSelectDtoFactory.federalStateSelectDto;
import static java.util.Objects.requireNonNullElse;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

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

        final FederalStateSettings federalStateSettings = settingsService.getFederalStateSettings();
        final LockTimeEntriesSettings lockTimeEntriesSettings = settingsService.getLockTimeEntriesSettings();
        final Optional<SubtractBreakFromTimeEntrySettings> subtractBreakFromTimeEntrySettings = settingsService.getSubtractBreakFromTimeEntrySettings();
        final SettingsDto settingsDto = toSettingsDto(federalStateSettings, lockTimeEntriesSettings, subtractBreakFromTimeEntrySettings.orElse(null));

        prepareModel(model, locale, settingsDto);

        return "settings/settings";
    }

    @PostMapping
    ModelAndView saveSettings(
        @ModelAttribute(ATTRIBUTE_NAME_SETTINGS) SettingsDto settingsDto,
        BindingResult bindingResult,
        @RequestParam(name = "preview", required = false) Optional<String> preview,
        Model model,
        Locale locale
    ) {

        settingsDtoValidator.validate(settingsDto, bindingResult);

        final Boolean subtractBreakFromTimeEntryIsActive = settingsDto.subtractBreakFromTimeEntryIsActive();

        if (preview.isPresent()) {
            prepareModel(model, locale, settingsDto);
            return new ModelAndView("settings/settings", model.asMap(), OK);
        }

        if (bindingResult.hasErrors()) {
            prepareModel(model, locale, settingsDto);
            return new ModelAndView("settings/settings", model.asMap(), UNPROCESSABLE_ENTITY);
        }

        settingsService.updateFederalStateSettings(settingsDto.federalState(), settingsDto.worksOnPublicHoliday());

        final int lockTimeEntriesDaysInPast = requireNonNullElse(settingsDto.lockTimeEntriesDaysInPastAsNumber(), -1);
        settingsService.updateLockTimeEntriesSettings(settingsDto.lockingIsActive(), lockTimeEntriesDaysInPast);

        if (subtractBreakFromTimeEntryIsActive != null) {
            final LocalDate date = settingsDto.subtractBreakFromTimeEntryActiveDate();

            final Instant timestamp;
            if (date == null) {
                timestamp = null;
            } else {
                final ZoneId zoneId = userSettingsProvider.zoneId();
                timestamp = date.atStartOfDay(zoneId).toInstant();
            }

            settingsService.updateSubtractBreakFromTimeEntrySettings(subtractBreakFromTimeEntryIsActive, timestamp);
        }

        return new ModelAndView("redirect:/settings");
    }

    private void prepareModel(Model model, Locale locale, SettingsDto settingsDto) {
        model.addAttribute(ATTRIBUTE_NAME_SETTINGS, settingsDto);
        model.addAttribute("federalStateSelect", federalStateSelectDto(settingsDto.federalState()));
        model.addAttribute("timeslotLockedExampleDate", getTimeslotLockedExampleDate(settingsDto, locale));
    }

    private SettingsDto toSettingsDto(
        FederalStateSettings federalStateSettings,
        LockTimeEntriesSettings lockTimeEntriesSettings,
        @Nullable SubtractBreakFromTimeEntrySettings subtractBreakFromTimeEntrySettings
    ) {

        final ZoneId userZoneId = userSettingsProvider.zoneId();

        final int lockTimeEntriesDaysInPast = lockTimeEntriesSettings.lockTimeEntriesDaysInPast();

        final Boolean subtractBreakFromTimeEntryIsActive = subtractBreakFromTimeEntrySettings == null
            ? null
            : subtractBreakFromTimeEntrySettings.subtractBreakFromTimeEntryIsActive();

        final LocalDate subtractBreakFromTimeEntryIsActiveDate = subtractBreakFromTimeEntrySettings == null
            ? null
            : subtractBreakFromTimeEntrySettings.timestampAsLocalDate(userZoneId).orElse(null);

        return new SettingsDto(
            federalStateSettings.federalState(),
            federalStateSettings.worksOnPublicHoliday(),
            lockTimeEntriesSettings.lockingIsActive(),
            lockTimeEntriesDaysInPast > -1 ? String.valueOf(lockTimeEntriesDaysInPast) : null,
            subtractBreakFromTimeEntryIsActive,
            subtractBreakFromTimeEntryIsActiveDate
        );
    }

    private String getTimeslotLockedExampleDate(SettingsDto settingsDto, Locale locale) {

        final int lockedDaysInPast = requireNonNullElse(settingsDto.lockTimeEntriesDaysInPastAsNumber(), -1);

        final ZoneId userZoneId = userSettingsProvider.zoneId();
        final LocalDate today = LocalDate.now(clock.withZone(userZoneId));
        return today.minusDays(lockedDaysInPast + 1L).format(DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", locale));
    }
}
