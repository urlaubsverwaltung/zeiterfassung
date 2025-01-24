package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.data.history.EntityRevisionMetadata;
import de.focusshift.zeiterfassung.user.HasUserIdComposite;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.slf4j.Logger;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class TimeEntryEditModalHelper {

    private static final Logger LOG = getLogger(lookup().lookupClass());
    private static final String TIME_ENTRY_HISTORY_MODEL_NAME = "timeEntryHistory";

    private final TimeEntryService timeEntryService;
    private final TimeEntryViewHelper timeEntryViewHelper;
    private final UserSettingsProvider userSettingsProvider;
    private final UserManagementService userManagementService;

    TimeEntryEditModalHelper(TimeEntryService timeEntryService, TimeEntryViewHelper timeEntryViewHelper, UserSettingsProvider userSettingsProvider, UserManagementService userManagementService) {
        this.timeEntryService = timeEntryService;
        this.timeEntryViewHelper = timeEntryViewHelper;
        this.userSettingsProvider = userSettingsProvider;
        this.userManagementService = userManagementService;
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

                    final ZoneId zoneId = userSettingsProvider.zoneId();
                    final List<TimeEntryHistoryItem> revisions = history.revisions();

                    final List<UserId> userIds = revisions.stream().map(TimeEntryHistoryItem::metadata)
                        .map(EntityRevisionMetadata::modifiedBy)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList();

                    final Map<UserId, User> userById = userManagementService.findAllUsersByIds(userIds).stream()
                        .collect(toMap(HasUserIdComposite::userId, identity()));

                    final List<TimeEntryHistoryItemDto> historyItemDtos = revisions
                        .stream()
                        .map(item -> new TimeEntryHistoryItemDto(
                            item.metadata().modifiedBy().map(userById::get).map(User::fullName).orElse(""),
                            item.metadata().entityRevisionType(),
                            LocalDate.ofInstant(item.metadata().modifiedAt(), zoneId),
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
