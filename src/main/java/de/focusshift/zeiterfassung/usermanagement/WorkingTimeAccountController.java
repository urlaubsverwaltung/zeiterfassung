package de.focusshift.zeiterfassung.usermanagement;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.settings.FederalStateSettings;
import de.focusshift.zeiterfassung.settings.FederalStateSettingsService;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.workingtime.WorkingTime;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.settings.FederalStateSelectDtoFactory.federalStateMessageKey;
import static de.focusshift.zeiterfassung.usermanagement.UserManagementController.hasAuthority;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static java.time.ZoneOffset.UTC;
import static org.springframework.util.StringUtils.hasText;

/**
 * Controller for the working time overview page of a person.
 */
@Controller
@RequestMapping("/users/{userId}/working-time")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_WORKING_TIME_EDIT_ALL')")
class WorkingTimeAccountController implements HasTimeClock, HasLaunchpad {

    private final UserManagementService userManagementService;
    private final WorkingTimeService workingTimeService;
    private final FederalStateSettingsService federalStateSettingsService;
    private final Clock clock;

    WorkingTimeAccountController(
        UserManagementService userManagementService,
        WorkingTimeService workingTimeService,
        FederalStateSettingsService federalStateSettingsService,
        Clock clock
    ) {
        this.userManagementService = userManagementService;
        this.workingTimeService = workingTimeService;
        this.federalStateSettingsService = federalStateSettingsService;
        this.clock = clock;
    }

    @GetMapping
    String get(@PathVariable("userId") Long userId, Model model,
               @RequestParam(value = "query", required = false, defaultValue = "") String query,
               @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
               @CurrentSecurityContext SecurityContext securityContext) {

        // workingTimes are whether the legacy default workingTime Monday to Friday with 8 hours
        // OR all user defined working times
        final List<WorkingTime> workingTimes = workingTimeService.getAllWorkingTimesByUser(new UserLocalId(userId)).stream()
            .filter(workingTime -> isGivenByUser(workingTime) || !workingTime.actualWorkingDays().isEmpty())
            .toList();

        final List<WorkingTimeListEntryDto> workingTimeDtos = workingTimesToDtos(workingTimes);
        final FederalStateSettings federalStateSettings = federalStateSettingsService.getFederalStateSettings();

        prepareGetWorkingTimesModel(model, query, userId, workingTimeDtos, securityContext, federalStateSettings);

        if (hasText(turboFrame)) {
            return "usermanagement/users::#" + turboFrame;
        } else {
            return "usermanagement/users";
        }
    }

    private boolean isGivenByUser(WorkingTime workingTime) {
        // workingTime with a validFrom date is considered as given by user
        // because it is a mandatory field when creating a working time
        return  workingTime.validFrom().isPresent();
    }

    private void prepareGetWorkingTimesModel(
        Model model, String query, Long userId,
        List<WorkingTimeListEntryDto> workingTimeDtos,
        SecurityContext securityContext,
        FederalStateSettings federalStateSettings
    ) {

        final List<UserDto> users = userManagementService.findAllUsers(query)
            .stream()
            .map(UserManagementController::userToDto)
            .toList();

        final UserDto selectedUser = users.stream().filter(u -> u.id() == userId)
            .findFirst()
            .or(() -> userManagementService.findUserByLocalId(new UserLocalId(userId)).map(UserManagementController::userToDto))
            .orElseThrow(() -> new IllegalArgumentException("could not find person=%s".formatted(userId)));

        final FederalState globalFederalState = federalStateSettings.federalState();

        model.addAttribute("query", query);
        model.addAttribute("slug", "working-time");
        model.addAttribute("users", users);
        model.addAttribute("selectedUser", selectedUser);
        model.addAttribute("workingTimes", workingTimeDtos);
        model.addAttribute("globalFederalState", globalFederalState);
        model.addAttribute("globalFederalStateMessageKey", federalStateMessageKey(globalFederalState));
        model.addAttribute("personSearchFormAction", "/users/" + selectedUser.id());

        model.addAttribute("allowedToEditWorkingTime", hasAuthority(ZEITERFASSUNG_WORKING_TIME_EDIT_ALL, securityContext));
        model.addAttribute("allowedToEditOvertimeAccount", hasAuthority(ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL, securityContext));
        model.addAttribute("allowedToEditPermissions", hasAuthority(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL, securityContext));
    }

    private List<WorkingTimeListEntryDto> workingTimesToDtos(List<WorkingTime> workingTimes) {
        return workingTimes.stream().map(this::workingTimeListEntryDto).toList();
    }

    private WorkingTimeListEntryDto workingTimeListEntryDto(WorkingTime workingTime) {

        final LocalDate today = LocalDate.now(clock);
        final Date validFrom = workingTime.validFrom().map(from -> Date.from(from.atStartOfDay().toInstant(UTC))).orElse(null);
        final Date validTo = workingTime.validTo().map(to -> Date.from(to.atStartOfDay().toInstant(UTC))).orElse(null);
        final Boolean validFromIsPast = workingTime.validFrom().map(from -> from.isBefore(today)).orElse(true);
        final String federalStateMessageKey = federalStateMessageKey(workingTime.individualFederalState());

        return new WorkingTimeListEntryDto(
            workingTime.id().value(),
            workingTime.userLocalId().value(),
            validFrom,
            validTo,
            validFromIsPast,
            workingTime.isCurrent(),
            workingTime.validFrom().isPresent(),
            federalStateMessageKey,
            workingTime.worksOnPublicHoliday(),
            workingTime.getMonday().hoursDoubleValue(),
            workingTime.getTuesday().hoursDoubleValue(),
            workingTime.getWednesday().hoursDoubleValue(),
            workingTime.getThursday().hoursDoubleValue(),
            workingTime.getFriday().hoursDoubleValue(),
            workingTime.getSaturday().hoursDoubleValue(),
            workingTime.getSunday().hoursDoubleValue()
        );
    }
}
