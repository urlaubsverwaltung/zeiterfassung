package de.focusshift.zeiterfassung.settings;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.USER_SEARCH_QUERY_PARAM;
import static de.focusshift.zeiterfassung.settings.FederalStateSelectDtoFactory.federalStateSelectDto;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static java.util.Objects.requireNonNullElse;
import static de.focusshift.zeiterfassung.settings.WorkingTimeSettings.DEFAULT_TIME_ROUNDING_MINUTES;
import static de.focusshift.zeiterfassung.settings.WorkingTimeSettings.DEFAULT_MIN_SUGGESTED_MINUTES;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@Controller
@RequestMapping("/settings")
@PreAuthorize("hasAnyAuthority('ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL', 'ZEITERFASSUNG_SETTINGS_GLOBAL')")
class SettingsController implements HasLaunchpad, HasTimeClock, HasUserSearch {

    private static final String ATTRIBUTE_NAME_SETTINGS = "settings";

    private final SettingsService settingsService;
    private final SettingsDtoValidator settingsDtoValidator;
    private final UserSettingsProvider userSettingsProvider;
    private final UserSearchViewHelper userSearchViewHelper;
    private final Clock clock;

    SettingsController(
        SettingsService settingsService,
        SettingsDtoValidator settingsDtoValidator,
        UserSettingsProvider userSettingsProvider,
        UserSearchViewHelper userSearchViewHelper,
        Clock clock
    ) {
        this.settingsService = settingsService;
        this.settingsDtoValidator = settingsDtoValidator;
        this.userSettingsProvider = userSettingsProvider;
        this.userSearchViewHelper = userSearchViewHelper;
        this.clock = clock;
    }

    @GetMapping
    String getSettings(Model model, Locale locale) {

        final FederalStateSettings federalStateSettings = settingsService.getFederalStateSettings();
        final WorkingTimeSettings workingTimeSettings = settingsService.getWorkingTimeSettings();
        final LockTimeEntriesSettings lockTimeEntriesSettings = settingsService.getLockTimeEntriesSettings();
        final Optional<SubtractBreakFromTimeEntrySettings> subtractBreakFromTimeEntrySettings = settingsService.getSubtractBreakFromTimeEntrySettings();
        final OooCalendarSettings oooCalendarSettings = settingsService.getOooCalendarSettings();
        final SettingsDto settingsDto = toSettingsDto(federalStateSettings, workingTimeSettings, lockTimeEntriesSettings, subtractBreakFromTimeEntrySettings.orElse(null), oooCalendarSettings);

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
            return new ModelAndView("settings/settings", model.asMap(), UNPROCESSABLE_CONTENT);
        }

        settingsService.updateFederalStateSettings(settingsDto.federalState(), settingsDto.worksOnPublicHoliday());
        settingsService.updateWorkingTimeSettings(
            dtoToWorkdays(settingsDto),
            requireNonNullElse(settingsDto.timeRoundingMinutes(), DEFAULT_TIME_ROUNDING_MINUTES),
            requireNonNullElse(settingsDto.minSuggestedMinutes(),  DEFAULT_MIN_SUGGESTED_MINUTES)
        );

        final int lockTimeEntriesDaysInPast = requireNonNullElse(settingsDto.lockTimeEntriesDaysInPastAsNumber(), -1);
        settingsService.updateLockTimeEntriesSettings(settingsDto.lockingIsActive(), lockTimeEntriesDaysInPast);

        settingsService.updateOooCalendarSettings(settingsDto.oooCalendarUrl());

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

    @GetMapping(params = USER_SEARCH_QUERY_PARAM, headers = TURBO_FRAME_HEADER)
    ModelAndView userSearchFragment(@RequestParam(USER_SEARCH_QUERY_PARAM) String query, @CurrentUser CurrentOidcUser currentUser, Model model) {
        return userSearchViewHelper.getSuggestionFragment(query, currentUser, model,
            suggestion -> {
                if (suggestion.userIdComposite().equals(currentUser.getUserIdComposite())) {
                    return "/timeentries";
                } else {
                    return "/timeentries/users/%s".formatted(suggestion.userLocalId().value());
                }
            }
        );
    }

    private void prepareModel(Model model, Locale locale, SettingsDto settingsDto) {
        model.addAttribute(ATTRIBUTE_NAME_SETTINGS, settingsDto);
        model.addAttribute("federalStateSelect", federalStateSelectDto(settingsDto.federalState()));
        model.addAttribute("timeslotLockedExampleDate", getTimeslotLockedExampleDate(settingsDto, locale));
    }

    private SettingsDto toSettingsDto(
        FederalStateSettings federalStateSettings,
        WorkingTimeSettings workingTimeSettings,
        LockTimeEntriesSettings lockTimeEntriesSettings,
        @Nullable SubtractBreakFromTimeEntrySettings subtractBreakFromTimeEntrySettings,
        OooCalendarSettings oooCalendarSettings
    ) {
        final ZoneId userZoneId = userSettingsProvider.zoneId();
        final int lockTimeEntriesDaysInPast = lockTimeEntriesSettings.lockTimeEntriesDaysInPast();
        final Boolean subtractBreakFromTimeEntryIsActive = subtractBreakFromTimeEntrySettings == null
            ? null : subtractBreakFromTimeEntrySettings.subtractBreakFromTimeEntryIsActive();
        final LocalDate subtractBreakFromTimeEntryIsActiveDate = subtractBreakFromTimeEntrySettings == null
            ? null : subtractBreakFromTimeEntrySettings.timestampAsLocalDate(userZoneId).orElse(null);

        final List<String> checkedWorkdays = workingTimeSettings.workdays().entrySet().stream()
            .filter(e -> !e.getValue().isZero())
            .map(e -> e.getKey().name().toLowerCase())
            .toList();
        final double workingTimeHours = workingTimeSettings.workdays().values().stream()
            .filter(d -> !d.isZero())
            .mapToDouble(d -> d.toMinutes() / 60.0)
            .findFirst()
            .orElse(0.0);

        return new SettingsDto(
            federalStateSettings.federalState(),
            federalStateSettings.worksOnPublicHoliday(),
            checkedWorkdays,
            workingTimeHours == 0 ? null : workingTimeHours,
            workingTimeSettings.timeRoundingMinutes(),
            workingTimeSettings.minSuggestedMinutes(),
            lockTimeEntriesSettings.lockingIsActive(),
            lockTimeEntriesDaysInPast > -1 ? String.valueOf(lockTimeEntriesDaysInPast) : null,
            subtractBreakFromTimeEntryIsActive,
            subtractBreakFromTimeEntryIsActiveDate,
            oooCalendarSettings.calendarUrl()
        );
    }

    private static EnumMap<DayOfWeek, Duration> dtoToWorkdays(SettingsDto dto) {
        final List<String> checked = dto.workday() != null ? dto.workday() : List.of();
        final Double hours = dto.workingTime();
        final Duration dayDuration = hours != null && hours > 0
            ? hoursToDuration(BigDecimal.valueOf(hours))
            : Duration.ZERO;
        final EnumMap<DayOfWeek, Duration> map = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            map.put(day, checked.contains(day.name().toLowerCase()) ? dayDuration : Duration.ZERO);
        }
        return map;
    }

    private static Duration hoursToDuration(BigDecimal hours) {
        final long totalMinutes = hours.multiply(BigDecimal.valueOf(60)).longValue();
        return Duration.ofMinutes(totalMinutes);
    }

    private String getTimeslotLockedExampleDate(SettingsDto settingsDto, Locale locale) {

        final int lockedDaysInPast = requireNonNullElse(settingsDto.lockTimeEntriesDaysInPastAsNumber(), -1);

        final ZoneId userZoneId = userSettingsProvider.zoneId();
        final LocalDate today = LocalDate.now(clock.withZone(userZoneId));
        return today.minusDays(lockedDaysInPast + 1L).format(DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", locale));
    }
}
