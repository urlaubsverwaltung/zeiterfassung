package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.data.history.EntityRevisionMetadata;
import de.focusshift.zeiterfassung.user.HasUserIdComposite;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class TimeEntryDialogHelper {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final String TIME_ENTRY_MODEL_NAME = "timeEntry";
    private static final String TIME_ENTRY_DIALOG_MODEL_NAME = "timeEntryDialog";

    private final TimeEntryService timeEntryService;
    private final TimeEntryViewHelper timeEntryViewHelper;
    private final UserSettingsProvider userSettingsProvider;
    private final UserManagementService userManagementService;

    public TimeEntryDialogHelper(TimeEntryService timeEntryService, TimeEntryViewHelper timeEntryViewHelper,
                                 UserSettingsProvider userSettingsProvider, UserManagementService userManagementService) {
        this.timeEntryService = timeEntryService;
        this.timeEntryViewHelper = timeEntryViewHelper;
        this.userSettingsProvider = userSettingsProvider;
        this.userManagementService = userManagementService;
    }

    public void addTimeEntryEditToModel(Model model, Long timeEntryId, String cancelFormAction) {

        final TimeEntry timeEntry = timeEntryService.findTimeEntry(timeEntryId)
            .orElseThrow(() -> new IllegalStateException("Could not find timeEntry with id=%d".formatted(timeEntryId)));

        addTimeEntry(model, timeEntry);
        addTimeEntryDialog(model, timeEntry, cancelFormAction);
    }

    private void addTimeEntry(Model model, TimeEntry timeEntry) {
        // timeEntry could already exist in model in case of a POST-Redirect-GET after form validation errors
        // we must not add it again, otherwise user input and BindingResult/Errors are lost.
        if (!model.containsAttribute(TIME_ENTRY_MODEL_NAME)) {
            final TimeEntryDTO timeEntryDto = timeEntryViewHelper.toTimeEntryDto(timeEntry);
            model.addAttribute(TIME_ENTRY_MODEL_NAME, timeEntryDto);
        }
    }

    private void addTimeEntryDialog(Model model, TimeEntry timeEntry, String cancelFormAction) {

        final User timeEntryUser = userManagementService.findUserById(timeEntry.userIdComposite().id())
            .orElseThrow(() -> new IllegalStateException("Could not find user with id=%d".formatted(timeEntry.id().value())));

        final List<TimeEntryHistoryItemDto> historyItems = getHistory(timeEntry.id());

        final TimeEntryDialogDto timeEntryDialogDto = new TimeEntryDialogDto(
            timeEntryUser.fullName(),
            historyItems,
            cancelFormAction
        );

        model.addAttribute(TIME_ENTRY_DIALOG_MODEL_NAME, timeEntryDialogDto);
    }

    private List<TimeEntryHistoryItemDto> getHistory(TimeEntryId timeEntryId) {
        return timeEntryService.findTimeEntryHistory(timeEntryId)
            .map(history -> {

                final ZoneId zoneId = userSettingsProvider.zoneId();
                final List<TimeEntryHistoryItem> revisions = history.revisions();

                final List<UserId> userIds = revisions.stream().map(TimeEntryHistoryItem::metadata)
                    .map(EntityRevisionMetadata::modifiedBy)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

                final Map<UserId, User> userById = userManagementService.findAllUsersByIds(userIds).stream()
                    .collect(toMap(HasUserIdComposite::userId, identity()));

                return revisions.stream()
                    .map(item -> timeEntryHistoryItemDto(item, userById::get, zoneId))
                    .toList();
            }).orElseGet(() -> {
                LOG.error("Could find history for timeEntry with id={}. But seems to be required for view rendering.", timeEntryId);
                return List.of();
            });
    }

    private TimeEntryHistoryItemDto timeEntryHistoryItemDto(TimeEntryHistoryItem historyItem, Function<UserId, User> userSupplier, ZoneId zoneId) {

        final EntityRevisionMetadata metadata = historyItem.metadata();

        final String username = metadata.modifiedBy().map(userSupplier).map(User::fullName).orElse("");
        final LocalDate date = LocalDate.ofInstant(metadata.modifiedAt(), zoneId);
        final TimeEntryDTO timeEntryDto = timeEntryViewHelper.toTimeEntryDto(historyItem.timeEntry());

        return new TimeEntryHistoryItemDto(username, metadata.entityRevisionType(), date, timeEntryDto);
    }
}