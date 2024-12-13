package de.focusshift.zeiterfassung.timeentry;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Component
public class TimeEntryEditModalHelper {

    private final TimeEntryService timeEntryService;
    private final TimeEntryViewHelper timeEntryViewHelper;

    TimeEntryEditModalHelper(TimeEntryService timeEntryService, TimeEntryViewHelper timeEntryViewHelper) {
        this.timeEntryService = timeEntryService;
        this.timeEntryViewHelper = timeEntryViewHelper;
    }

    public void addTimeEntryEditToModel(Model model, Long timeEntryId) {
        addTimeEntry(model, timeEntryId);
        addTimeEntryHistory(model, timeEntryId);
    }

    public void saveTimeEntry(TimeEntryDTO timeEntryDTO, BindingResult errors, Model model, RedirectAttributes redirectAttributes, OidcUser oidcUser) {
        timeEntryViewHelper.saveTimeEntry(timeEntryDTO, errors, model, redirectAttributes, oidcUser);
    }

    private void addTimeEntry(Model model, Long timeEntryId) {

        // timeEntry could already exist in model in case of a POST-Redirect-GET after form validation errors
        // we must not add it again, otherwise user input and BindingResult/Errors are lost.
        if (!model.containsAttribute("timeEntry")) {

            final TimeEntry timeEntry = timeEntryService.findTimeEntry(timeEntryId)
                .orElseThrow(() -> new IllegalStateException("Could not find timeEntry with id=%d".formatted(timeEntryId)));

            timeEntryViewHelper.addTimeEntryToModel(model, timeEntry);
        }
    }

    private void addTimeEntryHistory(Model model, Long timeEntryId) {
        if (!model.containsAttribute("timeEntryHistory")) {
            model.addAttribute("timeEntryHistory", List.of());
        }
    }
}
