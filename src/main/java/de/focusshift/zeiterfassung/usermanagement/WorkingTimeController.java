package de.focusshift.zeiterfassung.usermanagement;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
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

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.usermanagement.UserManagementController.hasAuthority;
import static java.math.BigDecimal.ZERO;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.util.StringUtils.hasText;

@Controller
@RequestMapping("/users/{userId}/working-time")
@PreAuthorize("hasAuthority('ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL')")
class WorkingTimeController implements HasTimeClock, HasLaunchpad {

    private final UserManagementService userManagementService;
    private final WorkingTimeService workingTimeService;
    private final WorkingTimeDtoValidator validator;

    WorkingTimeController(UserManagementService userManagementService, WorkingTimeService workingTimeService, WorkingTimeDtoValidator validator) {
        this.userManagementService = userManagementService;
        this.workingTimeService = workingTimeService;
        this.validator = validator;
    }

    @GetMapping
    String get(@PathVariable("userId") Long userId, Model model,
               @RequestParam(value = "query", required = false, defaultValue = "") String query,
               @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
               @AuthenticationPrincipal OidcUser principal) {

        final List<WorkingTime> workingTimes = workingTimeService.getAllWorkingTimesByUser(new UserLocalId(userId));
        final List<WorkingTimeDto> workingTimeDtos = workingTimesToDtos(workingTimes);

        prepareGetWorkingTimesModel(model, query, userId, workingTimeDtos, principal);

        if (hasText(turboFrame)) {
            return "usermanagement/users::#" + turboFrame;
        } else {
            return "usermanagement/users";
        }
    }

    @GetMapping("/new")
    String newWorkingTime(@PathVariable("userId") Long userId, Model model,
                          @RequestParam(value = "query", required = false, defaultValue = "") String query,
                          @AuthenticationPrincipal OidcUser principal) {

        final WorkingTimeDto workingTimeDto = new WorkingTimeDto();

        prepareWorkingTimeCreateOrEditModel(model, query, userId, workingTimeDto, principal);
        model.addAttribute("createMode", true);

        return "usermanagement/users";
    }

    @GetMapping("/{workingTimeId}")
    String getWorkingTime(@PathVariable("userId") Long userId, Model model,
                          @PathVariable("workingTimeId") String workingTimeId,
                          @RequestParam(value = "query", required = false, defaultValue = "") String query,
                          @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
                          @AuthenticationPrincipal OidcUser principal) {

        final WorkingTime workingTime = workingTimeService.getWorkingTimeById(WorkingTimeId.fromString(workingTimeId))
            // TODO nice frontend message/page instead of "no-content" in case of ajax call
            .orElseThrow(() -> new IllegalStateException("could not find working time with id=" + workingTimeId));

        final WorkingTimeDto workingTimeDto = workingTimeToDto(workingTime);

        prepareWorkingTimeCreateOrEditModel(model, query, userId, workingTimeDto, principal);
        model.addAttribute("createMode", false);

        if (hasText(turboFrame)) {
            return "usermanagement/users::#" + turboFrame;
        } else {
            return "usermanagement/users";
        }
    }

    @PostMapping({"/new", "/{workingTimeId}"})
    ModelAndView createNewWorkingTime(@PathVariable("userId") Long userId, Model model,
                                      @ModelAttribute("workingTime") WorkingTimeDto workingTimeDto, BindingResult result,
                                      @RequestParam(value = "query", required = false, defaultValue = "") String query,
                                      @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
                                      @AuthenticationPrincipal OidcUser principal,
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
            prepareWorkingTimeCreateOrEditModel(model, query, userId, workingTimeDto, principal);
            model.addAttribute("createMode", workingTimeDto.getId() == null);
            if (hasText(turboFrame)) {
                return new ModelAndView("usermanagement/users::#" + turboFrame, UNPROCESSABLE_ENTITY);
            } else {
                return new ModelAndView("usermanagement/users");
            }
        }

        final WorkWeekUpdate workWeekUpdate = dtoToWorkWeekUpdate(workingTimeDto);
        workingTimeService.updateWorkingTime(WorkingTimeId.fromString(workingTimeDto.getId()), workWeekUpdate);

        return new ModelAndView("redirect:/users/%s/working-time".formatted(userId));
    }

    private void prepareGetWorkingTimesModel(Model model, String query, Long userId, List<WorkingTimeDto> workingTimeDtos,
                                             OidcUser principal) {

        final List<UserDto> users = userManagementService.findAllUsers(query)
            .stream()
            .map(UserManagementController::userToDto)
            .toList();

        final UserDto selectedUser = users.stream().filter(u -> u.id() == userId)
            .findFirst()
            .or(() -> userManagementService.findUserByLocalId(new UserLocalId(userId)).map(UserManagementController::userToDto))
            .orElseThrow(() -> new IllegalArgumentException("could not find person=%s".formatted(userId)));

        model.addAttribute("query", query);
        model.addAttribute("slug", "working-time");
        model.addAttribute("users", users);
        model.addAttribute("selectedUser", selectedUser);
        model.addAttribute("workingTimes", workingTimeDtos);
        model.addAttribute("personSearchFormAction", "/users/" + selectedUser.id());

        model.addAttribute("allowedToEditWorkingTime", hasAuthority(ZEITERFASSUNG_WORKING_TIME_EDIT_ALL, principal));
        model.addAttribute("allowedToEditOvertimeAccount", hasAuthority(ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL, principal));
    }

    private void prepareWorkingTimeCreateOrEditModel(Model model, String query, Long userId, WorkingTimeDto workingTimeDto, OidcUser principal) {

        prepareGetWorkingTimesModel(model, query, userId, List.of(), principal);

        model.addAttribute("section", "working-time-edit");
        model.addAttribute("workingTime", workingTimeDto);
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

    private static List<WorkingTimeDto> workingTimesToDtos(List<WorkingTime> workingTimes) {
        return workingTimes.stream().map(WorkingTimeController::workingTimeToDto).toList();
    }

    private static WorkingTimeDto workingTimeToDto(WorkingTime workingTime) {

        final WorkingTimeDto.Builder builder = WorkingTimeDto.builder();

        if (workingTime.hasDifferentWorkingHours()) {
            final Map<DayOfWeek, Consumer<Double>> setter = Map.of(
                MONDAY, builder::workingTimeMonday,
                TUESDAY, builder::workingTimeTuesday,
                WEDNESDAY, builder::workingTimeWednesday,
                THURSDAY, builder::workingTimeThursday,
                FRIDAY, builder::workingTimeFriday,
                SATURDAY, builder::workingTimeSaturday,
                SUNDAY, builder::workingTimeSunday
            );
            for (WorkDay workingDay : workingTime.getWorkingDays()) {
                setter.get(workingDay.dayOfWeek()).accept(workingDay.hours().doubleValue());
            }
        } else {
            // every day has the same hours
            // -> individual input fields should be empty
            // -> working time input should be set
            builder.workingTime(workingTime.getWorkingDays().get(0).hours().doubleValue());
        }

        return builder
            .id(workingTime.id().value())
            // TODO validFrom
            .validFrom(null)
            .userId(workingTime.userIdComposite().localId().value())
            .workday(workingTime.getWorkingDays().stream().map(WorkDay::dayOfWeek).toList())
            .build();
    }

    private WorkWeekUpdate dtoToWorkWeekUpdate(WorkingTimeDto workingTimeDto) {

        final WorkWeekUpdate.Builder builder = WorkWeekUpdate.builder()
            .validFrom(workingTimeDto.getValidFrom());

        final Map<String, Supplier<Double>> values = Map.of(
            "monday", workingTimeDto::getWorkingTimeMonday,
            "tuesday", workingTimeDto::getWorkingTimeTuesday,
            "wednesday", workingTimeDto::getWorkingTimeWednesday,
            "thursday", workingTimeDto::getWorkingTimeThursday,
            "friday", workingTimeDto::getWorkingTimeFriday,
            "saturday", workingTimeDto::getWorkingTimeSaturday,
            "sunday", workingTimeDto::getWorkingTimeSunday
        );

        final Map<String, Consumer<BigDecimal>> setter = Map.of(
            "monday", builder::monday,
            "tuesday", builder::tuesday,
            "wednesday", builder::wednesday,
            "thursday", builder::thursday,
            "friday", builder::friday,
            "saturday", builder::saturday,
            "sunday", builder::sunday
        );

        // for each day set the individual value or the common value when nothing is set
        for (String day : workingTimeDto.getWorkday()) {

            final BigDecimal hours = Optional.ofNullable(values.get(day).get())
                .or(() -> Optional.of(workingTimeDto.getWorkingTime()))
                .map(BigDecimal::new)
                .orElse(ZERO);

            setter.get(day).accept(hours);
        }

        return builder.build();
    }
}
