package de.focusshift.zeiterfassung.usermanagement;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.settings.FederalStateSettings;
import de.focusshift.zeiterfassung.settings.FederalStateSettingsService;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.workingtime.WorkingTime;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeId;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.settings.FederalStateSelectDtoFactory.federalStateMessageKey;
import static de.focusshift.zeiterfassung.settings.FederalStateSelectDtoFactory.federalStateSelectDto;
import static de.focusshift.zeiterfassung.usermanagement.UserManagementController.hasAuthority;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static de.focusshift.zeiterfassung.workingtime.WorkingTime.hoursToDuration;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.util.Objects.requireNonNullElseGet;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.util.StringUtils.hasText;

/**
 * Controller for a special working time of a person.
 */
@Controller
@RequestMapping("/users/{userId}/working-time")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_WORKING_TIME_EDIT_ALL')")
class WorkingTimeController implements HasTimeClock, HasLaunchpad {

    private final UserManagementService userManagementService;
    private final WorkingTimeService workingTimeService;
    private final WorkingTimeDtoValidator validator;
    private final FederalStateSettingsService federalStateSettingsService;

    WorkingTimeController(UserManagementService userManagementService,
                          WorkingTimeService workingTimeService,
                          WorkingTimeDtoValidator validator,
                          FederalStateSettingsService federalStateSettingsService) {

        this.userManagementService = userManagementService;
        this.workingTimeService = workingTimeService;
        this.validator = validator;
        this.federalStateSettingsService = federalStateSettingsService;
    }

    @GetMapping("/new")
    String newWorkingTime(@PathVariable("userId") Long userId, Model model,
                          @RequestParam(value = "query", required = false, defaultValue = "") String query,
                          @CurrentSecurityContext SecurityContext securityContext) {

        final WorkingTimeDto workingTimeDto = new WorkingTimeDto();
        workingTimeDto.setFederalState(FederalState.GLOBAL);
        workingTimeDto.setWorksOnPublicHoliday(null);

        prepareWorkingTimeCreateOrEditModel(model, query, userId, workingTimeDto, securityContext);
        model.addAttribute("createMode", true);

        return "usermanagement/users";
    }

    @GetMapping("/{workingTimeId}")
    String getWorkingTime(@PathVariable("userId") Long userId, Model model,
                          @PathVariable("workingTimeId") String workingTimeId,
                          @RequestParam(value = "query", required = false, defaultValue = "") String query,
                          @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                          @CurrentSecurityContext SecurityContext securityContext) {

        final WorkingTime workingTime = workingTimeService.getWorkingTimeById(WorkingTimeId.fromString(workingTimeId))
            .orElseThrow(() -> new IllegalStateException("could not find working time with id=" + workingTimeId));

        final WorkingTimeDto workingTimeDto = workingTimeToDto(workingTime);

        prepareWorkingTimeCreateOrEditModel(model, query, userId, workingTimeDto, securityContext);
        model.addAttribute("createMode", false);

        if (hasText(turboFrame)) {
            return "usermanagement/users::#" + turboFrame;
        } else {
            return "usermanagement/users";
        }
    }

    @PostMapping("/new")
    ModelAndView createNewWorkingTime(@PathVariable("userId") Long userId, Model model,
                                      @ModelAttribute("workingTime") WorkingTimeDto workingTimeDto, BindingResult result,
                                      @RequestParam(value = "query", required = false, defaultValue = "") String query,
                                      @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                                      @CurrentSecurityContext SecurityContext securityContext,
                                      @RequestParam Map<String, Object> requestParameters) {

        final Object select = requestParameters.get("select");
        if (select instanceof String selectValue) {
            // the <form> offers buttons to select a day without choosing the <input type=checkbox>
            // this button has been selected by the user -> add the workday
            workingTimeDto.getWorkday().add(selectValue);
        }

        final Object clear = requestParameters.get("clear");
        if (clear instanceof String clearValue) {
            // the <form> offers buttons to quickly clear a day
            // this button has been selected by the user -> clear the workday
            clearWorkDayHours(clearValue, workingTimeDto);
        }

        validator.validate(workingTimeDto, result);
        if (result.hasErrors()) {
            prepareWorkingTimeCreateOrEditModel(model, query, userId, workingTimeDto, securityContext);
            model.addAttribute("createMode", workingTimeDto.getId() == null);
            if ("person-frame".equals(turboFrame)) {
                return new ModelAndView("usermanagement/users::#person-frame", UNPROCESSABLE_ENTITY);
            } else {
                return new ModelAndView("usermanagement/users");
            }
        }

        final UserLocalId userLocalId = new UserLocalId(workingTimeDto.getUserId());
        final LocalDate validFrom = workingTimeDto.getValidFrom();
        final FederalState federalState = workingTimeDto.getFederalState();
        final Boolean worksOnPublicHoliday = workingTimeDto.getWorksOnPublicHoliday();
        final EnumMap<DayOfWeek, Duration> workdays = workingTimeDtoToWorkdays(workingTimeDto);

        workingTimeService.createWorkingTime(userLocalId, validFrom, federalState, worksOnPublicHoliday, workdays);

        return new ModelAndView("redirect:/users/%s/working-time".formatted(userId));
    }

    @PostMapping("/{workingTimeId}")
    ModelAndView updateWorkingTime(@PathVariable("userId") Long userId, Model model,
                                   @ModelAttribute("workingTime") WorkingTimeDto workingTimeDto, BindingResult result,
                                   @RequestParam(value = "query", required = false, defaultValue = "") String query,
                                   @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                                   @CurrentSecurityContext SecurityContext securityContext,
                                   @RequestParam Map<String, Object> requestParameters) {

        final Object select = requestParameters.get("select");
        if (select instanceof String selectValue) {
            // the <form> offers buttons to select a day without choosing the <input type=checkbox>
            // this button has been selected by the user -> add the workday
            workingTimeDto.getWorkday().add(selectValue);
        }

        final Object clear = requestParameters.get("clear");
        if (clear instanceof String clearValue) {
            // the <form> offers buttons to quickly clear a day
            // this button has been selected by the user -> clear the workday
            clearWorkDayHours(clearValue, workingTimeDto);
        }

        validator.validate(workingTimeDto, result);
        if (result.hasErrors()) {
            prepareWorkingTimeCreateOrEditModel(model, query, userId, workingTimeDto, securityContext);
            model.addAttribute("createMode", workingTimeDto.getId() == null);
            if (hasText(turboFrame)) {
                return new ModelAndView("usermanagement/users::#" + turboFrame, UNPROCESSABLE_ENTITY);
            } else {
                return new ModelAndView("usermanagement/users");
            }
        }

        final LocalDate validFrom = workingTimeDto.getValidFrom();
        final FederalState federalState = workingTimeDto.getFederalState();
        final Boolean worksOnPublicHoliday = workingTimeDto.getWorksOnPublicHoliday();
        final EnumMap<DayOfWeek, Duration> workdays = workingTimeDtoToWorkdays(workingTimeDto);
        workingTimeService.updateWorkingTime(WorkingTimeId.fromString(workingTimeDto.getId()), validFrom, federalState, worksOnPublicHoliday, workdays);

        return new ModelAndView("redirect:/users/%s/working-time".formatted(userId));
    }

    @PostMapping("/{workingTimeId}/delete")
    ModelAndView deleteWorkingTime(@PathVariable("userId") Long userId, @PathVariable("workingTimeId") UUID workingTimeId) {

        final boolean deleted = workingTimeService.deleteWorkingTime(new WorkingTimeId(workingTimeId));
        if (!deleted) {
            // delete button is not visible when working-time cannot be deleted
            // therefore just render error page and use BAD_REQUEST status code
            return new ModelAndView("error/5xx", BAD_REQUEST);
        }

        return new ModelAndView("redirect:/users/%s/working-time".formatted(userId));
    }

    private EnumMap<DayOfWeek, Duration> workingTimeDtoToWorkdays(WorkingTimeDto workingTimeDto) {

        final EnumMap<DayOfWeek, Boolean> checked = new EnumMap<>(Map.of(
            MONDAY, workingTimeDto.isWorkDayMonday(),
            TUESDAY, workingTimeDto.isWorkDayTuesday(),
            WEDNESDAY, workingTimeDto.isWorkDayWednesday(),
            THURSDAY, workingTimeDto.isWorkDayThursday(),
            FRIDAY, workingTimeDto.isWorkDayFriday(),
            SATURDAY, workingTimeDto.isWorkDaySaturday(),
            SUNDAY, workingTimeDto.isWorkDaySunday()
        ));

        final EnumMap<DayOfWeek, Supplier<Double>> dayWorkingTime = new EnumMap<>(Map.of(
            MONDAY, workingTimeDto::getWorkingTimeMonday,
            TUESDAY, workingTimeDto::getWorkingTimeTuesday,
            WEDNESDAY, workingTimeDto::getWorkingTimeWednesday,
            THURSDAY, workingTimeDto::getWorkingTimeThursday,
            FRIDAY, workingTimeDto::getWorkingTimeFriday,
            SATURDAY, workingTimeDto::getWorkingTimeSaturday,
            SUNDAY, workingTimeDto::getWorkingTimeSunday
        ));

        final ToDoubleFunction<DayOfWeek> duration = dayOfWeek -> checked.get(dayOfWeek)
            ? requireNonNullElseGet(dayWorkingTime.get(dayOfWeek).get(), workingTimeDto::getWorkingTime)
            : 0d;

        return new EnumMap<>(Map.of(
            MONDAY, hoursToDuration(duration.applyAsDouble(MONDAY)),
            TUESDAY, hoursToDuration(duration.applyAsDouble(TUESDAY)),
            WEDNESDAY, hoursToDuration(duration.applyAsDouble(WEDNESDAY)),
            THURSDAY, hoursToDuration(duration.applyAsDouble(THURSDAY)),
            FRIDAY, hoursToDuration(duration.applyAsDouble(FRIDAY)),
            SATURDAY, hoursToDuration(duration.applyAsDouble(SATURDAY)),
            SUNDAY, hoursToDuration(duration.applyAsDouble(SUNDAY))
        ));
    }

    private void prepareGetWorkingTimesModel(Model model, String query, Long userId,
                                             List<WorkingTimeListEntryDto> workingTimeDtos,
                                             SecurityContext securityContext,
                                             FederalStateSettings federalStateSettings) {

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

    private void prepareWorkingTimeCreateOrEditModel(Model model, String query, Long userId, WorkingTimeDto workingTimeDto, SecurityContext securityContext) {

        final FederalStateSettings federalStateSettings = federalStateSettingsService.getFederalStateSettings();

        prepareGetWorkingTimesModel(model, query, userId, List.of(), securityContext, federalStateSettings);

        model.addAttribute("section", "working-time-edit");
        model.addAttribute("workingTime", workingTimeDto);
        // number is required for the messages choice pattern which does not work with boolean
        model.addAttribute("globalWorksOnPublicHoliday", federalStateSettings.worksOnPublicHoliday() ? 1 : 0);
        model.addAttribute("federalStateSelect", federalStateSelectDto(workingTimeDto.getFederalState(), true));
    }

    private void clearWorkDayHours(String dayOfWeek, WorkingTimeDto workingTimeDto) {

        final Map<String, Consumer<Double>> values = Map.of(
            "monday", workingTimeDto::setWorkingTimeMonday,
            "tuesday", workingTimeDto::setWorkingTimeTuesday,
            "wednesday", workingTimeDto::setWorkingTimeWednesday,
            "thursday", workingTimeDto::setWorkingTimeThursday,
            "friday", workingTimeDto::setWorkingTimeFriday,
            "saturday", workingTimeDto::setWorkingTimeSaturday,
            "sunday", workingTimeDto::setWorkingTimeSunday
        );

        final Consumer<Double> consumer = values.get(dayOfWeek);
        if (consumer != null) {
            consumer.accept(0.0); // o.O`
        }
    }

    private static WorkingTimeDto workingTimeToDto(WorkingTime workingTime) {

        final WorkingTimeDto dto = new WorkingTimeDto();

        if (workingTime.hasDifferentWorkingHours()) {
            dto.setWorkingTimeMonday(workingTime.getMonday().hoursDoubleValue());
            dto.setWorkingTimeTuesday(workingTime.getTuesday().hoursDoubleValue());
            dto.setWorkingTimeWednesday(workingTime.getWednesday().hoursDoubleValue());
            dto.setWorkingTimeThursday(workingTime.getThursday().hoursDoubleValue());
            dto.setWorkingTimeFriday(workingTime.getFriday().hoursDoubleValue());
            dto.setWorkingTimeSaturday(workingTime.getSaturday().hoursDoubleValue());
            dto.setWorkingTimeSunday(workingTime.getSunday().hoursDoubleValue());
        } else {
            // every day has the same hours
            // -> individual input fields should be empty
            // -> working time input should be set
            dto.setWorkingTime(workingTime.getMonday().hoursDoubleValue());
        }

        dto.setId(workingTime.id().value());
        dto.setValidFrom(workingTime.validFrom().orElse(null));
        dto.setMinValidFrom(workingTime.minValidFrom().orElse(null));
        dto.setMaxValidFrom(workingTime.validTo().orElse(null));
        dto.setFederalState(workingTime.individualFederalState());
        dto.setWorksOnPublicHoliday(workingTime.individualWorksOnPublicHoliday().asBoolean());
        dto.setUserId(workingTime.userIdComposite().localId().value());
        dto.setWorkday(workingTime.actualWorkingDays().stream().map(DayOfWeek::name).map(String::toLowerCase).toList());

        return dto;
    }
}
