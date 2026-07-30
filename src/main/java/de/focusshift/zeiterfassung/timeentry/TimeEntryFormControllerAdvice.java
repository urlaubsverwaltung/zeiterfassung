package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.settings.TimeEntrySettings;
import de.focusshift.zeiterfassung.settings.TimeEntrySettingsService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {HasTimeEntryForm.class})
class TimeEntryFormControllerAdvice {

    private final TimeEntrySettingsService timeEntrySettingsService;

    TimeEntryFormControllerAdvice(TimeEntrySettingsService timeEntrySettingsService) {
        this.timeEntrySettingsService = timeEntrySettingsService;
    }

    @ModelAttribute
    public void addAttributes(Model model) {
        final TimeEntrySettings timeEntrySettings = timeEntrySettingsService.getTimeEntrySettings();
        model.addAttribute("timeEntrySettings", timeEntrySettings);
    }
}
