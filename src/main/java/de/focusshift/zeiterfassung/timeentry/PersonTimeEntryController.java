package de.focusshift.zeiterfassung.timeentry;

import ch.qos.logback.core.model.Model;
import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.user.DateFormatter;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import io.micrometer.core.instrument.Clock;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * {@link TimeEntry} related stuff managed by a privileged person for another person.
 */
@Controller
@RequestMapping("/person/{userLocalId}/timeentries")
class PersonTimeEntryController implements HasTimeClock, HasLaunchpad {

    private final TimeEntryService timeEntryService;
    private final UserManagementService userManagementService;
    private final UserSettingsProvider userSettingsProvider;
    private final DateFormatter dateFormatter;
    private final TimeEntryViewHelper timeEntryViewHelper;
    private final Clock clock;

    PersonTimeEntryController(TimeEntryService timeEntryService, UserManagementService userManagementService,
                              UserSettingsProvider userSettingsProvider, DateFormatter dateFormatter,
                              TimeEntryViewHelper timeEntryViewHelper, Clock clock) {
        this.timeEntryService = timeEntryService;
        this.userManagementService = userManagementService;
        this.userSettingsProvider = userSettingsProvider;
        this.dateFormatter = dateFormatter;
        this.timeEntryViewHelper = timeEntryViewHelper;
        this.clock = clock;
    }

    @GetMapping
    public String getPersonTimeEntries(@PathVariable("userLocalId") String userLocalId, Model model) {
        return "";
    }

    @PostMapping
    public String createPersonTimeEntry(@PathVariable("userLocalId") String userLocalId, Model model) {
        return "";
    }

    @PostMapping("/{timeEntryId}")
    public String updatePersonTimeEntry(@PathVariable("userLocalId") String userLocalId, Model model) {
        return "";
    }

    @PostMapping(value = "/{timeEntryId}", params = "delete")
    public String deletePersonTimeEntry(@PathVariable("userLocalId") String userLocalId, Model model) {
        return "";
    }
}
