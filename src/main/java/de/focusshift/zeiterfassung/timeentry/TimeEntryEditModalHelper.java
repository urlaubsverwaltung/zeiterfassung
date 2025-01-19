package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserId;
import org.slf4j.Logger;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class TimeEntryEditModalHelper {

    private static final Logger LOG = getLogger(lookup().lookupClass());
    private static final String TIME_ENTRY_HISTORY_MODEL_NAME = "timeEntryHistory";

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
        if (model.containsAttribute("timeEntry")) {
            return;
        }

        final TimeEntry timeEntry = timeEntryService.findTimeEntry(timeEntryId)
            .orElseThrow(() -> new IllegalStateException("Could not find timeEntry with id=%d".formatted(timeEntryId)));

        timeEntryViewHelper.addTimeEntryToModel(model, timeEntry);
    }

    private void addTimeEntryHistory(Model model, Long timeEntryId) {
        if (model.containsAttribute(TIME_ENTRY_HISTORY_MODEL_NAME)) {
            return;
        }

        timeEntryService.findTimeEntryHistory(new TimeEntryId(timeEntryId))
            .ifPresentOrElse(
                history -> {
                    final List<TimeEntryHistoryItemDto> historyItemDtos = history.revisions()
                        .stream()
                        .map(item -> new TimeEntryHistoryItemDto(
                            // TODO username / email / avatar
                            item.metadata().modifiedBy().map(UserId::value).map(String::valueOf).orElse(""),
                            // TODO translation of revision type name
                            item.metadata().entityRevisionType().name().toLowerCase(),
                            // TODO text represantation of instant mapped to user time zone
                            item.metadata().modifiedAt().toString(),
                            timeEntryViewHelper.toTimeEntryDto(item.timeEntry())
                        ))
                        .toList()
                        .reversed();
                    model.addAttribute(TIME_ENTRY_HISTORY_MODEL_NAME, historyItemDtos);
                },
                // TODO throw and show 5xx? probably there has to be one history item at least -> the CREATED item
                () -> LOG.error("Could find history for timeEntry with id={}. But seems to be required for view rendering.", timeEntryId)
            );
    }
}
