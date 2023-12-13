package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.timeentry.settings.TimeEntryFreeze;
import de.focusshift.zeiterfassung.timeentry.settings.TimeEntrySettings;
import de.focusshift.zeiterfassung.timeentry.settings.TimeEntrySettingsService;
import de.focusshift.zeiterfassung.web.html.HtmlOptionDto;
import de.focusshift.zeiterfassung.web.html.HtmlSelectDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/settings")
@PreAuthorize("hasAuthority('ROLE_ZEITERFASSUNG_SETTINGS_TIME_ENTRIES')")
class SettingsController {

    private final TimeEntrySettingsService timeEntrySettingsService;

    SettingsController(TimeEntrySettingsService timeEntrySettingsService) {
        this.timeEntrySettingsService = timeEntrySettingsService;
    }

    @GetMapping
    String getSettings(Model model) {

        final TimeEntrySettings timeEntrySettings = timeEntrySettingsService.getTimeEntrySettings();
        final SettingsDto settingsDto = toSettingsDto(timeEntrySettings);
        final TimeEntryFreeze.Unit unit = settingsDto.getTimeEntrySettings().getUnit();

        model.addAttribute("settings", settingsDto);
        model.addAttribute("timeEntryFreezeUnits", timeEntryFreezeUnits(unit));

        return "settings/settings";
    }

    @PostMapping
    String updateSettings(@ModelAttribute("settings") SettingsDto settingsDto, Errors errors, Model model) {

        if (errors.hasErrors()) {
            final TimeEntryFreeze.Unit unit = settingsDto.getTimeEntrySettings().getUnit();
            model.addAttribute("timeEntryFreezeUnits", timeEntryFreezeUnits(unit));
            return "settings/settings";
        }

        final TimeEntryFreeze timeEntryFreeze = toTimeEntryFreeze(settingsDto.getTimeEntrySettings());
        timeEntrySettingsService.updateTimeEntrySettings(timeEntryFreeze);

        return "redirect:/settings";
    }

    private static HtmlSelectDto timeEntryFreezeUnits(TimeEntryFreeze.Unit selectedUnit) {

        final List<HtmlOptionDto> options = Arrays.stream(TimeEntryFreeze.Unit.values())
            .map(unit -> new HtmlOptionDto(messageKey(unit), unit.name(), unit.equals(selectedUnit)))
            .toList();

        return HtmlSelectDto.withOptions(options);
    }

    private static String messageKey(TimeEntryFreeze.Unit unit) {
        return "settings.time-entry-freeze.unit.%s".formatted(unit.name());
    }

    private static SettingsDto toSettingsDto(TimeEntrySettings timeEntrySettings) {

        final TimeEntrySettingsDto timeEntrySettingsDto = new TimeEntrySettingsDto();
        timeEntrySettingsDto.setEnabled(timeEntrySettings.timeEntryFreeze().enabled());
        timeEntrySettingsDto.setValue(timeEntrySettingsDto.getValue());
        timeEntrySettingsDto.setUnit(timeEntrySettings.timeEntryFreeze().unit());

        final SettingsDto dto = new SettingsDto();
        dto.setTimeEntrySettings(timeEntrySettingsDto);

        return dto;
    }

    private static TimeEntryFreeze toTimeEntryFreeze(TimeEntrySettingsDto dto) {
        return new TimeEntryFreeze(dto.isEnabled(), dto.getValue(), dto.getUnit());
    }
}
