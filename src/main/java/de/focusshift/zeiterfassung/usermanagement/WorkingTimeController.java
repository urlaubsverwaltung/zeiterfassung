package de.focusshift.zeiterfassung.usermanagement;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.workingtime.WorkWeekUpdate;
import de.focusshift.zeiterfassung.workingtime.WorkingTime;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeId;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeService;
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

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.usermanagement.UserManagementController.hasAuthority;
import static de.focusshift.zeiterfassung.workingtime.WorkingTime.hoursToDuration;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNullElseGet;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.util.StringUtils.hasText;

@Controller
@RequestMapping("/users/{userId}/working-time")
@PreAuthorize("hasAuthority('ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL')")
class WorkingTimeController implements HasTimeClock, HasLaunchpad {

    private final UserManagementService userManagementService;
    private final WorkingTimeService workingTimeService;
    private final WorkingTimeDtoValidator validator;
    private final Clock clock;

    WorkingTimeController(UserManagementService userManagementService,
                          WorkingTimeService workingTimeService,
                          WorkingTimeDtoValidator validator,
                          Clock clock) {

        this.userManagementService = userManagementService;
        this.workingTimeService = workingTimeService;
        this.validator = validator;
        this.clock = clock;
    }

    @GetMapping
    String get(@PathVariable("userId") Long userId, Model model,
               @RequestParam(value = "query", required = false, defaultValue = "") String query,
               @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
               @AuthenticationPrincipal OidcUser principal) {

        final List<WorkingTime> workingTimes = workingTimeService.getAllWorkingTimesByUser(new UserLocalId(userId));
        final List<WorkingTimeListEntryDto> workingTimeDtos = workingTimesToDtos(workingTimes);

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

    @PostMapping("/new")
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

        final UserLocalId userLocalId = new UserLocalId(workingTimeDto.getUserId());
        final LocalDate validFrom = workingTimeDto.getValidFrom();
        final EnumMap<DayOfWeek, Duration> workdays = workingTimeDtoToWorkdays(workingTimeDto);

        workingTimeService.createWorkingTime(userLocalId, validFrom, workdays);

        return new ModelAndView("redirect:/users/%s/working-time".formatted(userId));
    }

    @PostMapping("/{workingTimeId}")
    ModelAndView updateWorkingTime(@PathVariable("userId") Long userId, Model model,
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

        final Function<DayOfWeek, Double> duration = dayOfWeek -> checked.get(dayOfWeek)
            ? requireNonNullElseGet(dayWorkingTime.get(dayOfWeek).get(), workingTimeDto::getWorkingTime)
            : 0d;

        return new EnumMap<>(Map.of(
            MONDAY, hoursToDuration(duration.apply(MONDAY)),
            TUESDAY, hoursToDuration(duration.apply(TUESDAY)),
            WEDNESDAY, hoursToDuration(duration.apply(WEDNESDAY)),
            THURSDAY, hoursToDuration(duration.apply(THURSDAY)),
            FRIDAY, hoursToDuration(duration.apply(FRIDAY)),
            SATURDAY, hoursToDuration(duration.apply(SATURDAY)),
            SUNDAY, hoursToDuration(duration.apply(SUNDAY))
        ));
    }

    private void prepareGetWorkingTimesModel(Model model, String query, Long userId,
                                             List<WorkingTimeListEntryDto> workingTimeDtos, OidcUser principal) {

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

    private List<WorkingTimeListEntryDto> workingTimesToDtos(List<WorkingTime> workingTimes) {
        return workingTimes.stream().map(this::workingTimeListEntryDto).toList();
    }

    private WorkingTimeListEntryDto workingTimeListEntryDto(WorkingTime workingTime) {

        final LocalDate today = LocalDate.now(clock);

        return new WorkingTimeListEntryDto(
            workingTime.id().value(),
            workingTime.userLocalId().value(),
            workingTime.validFrom().map(localDate -> Date.from(localDate.atStartOfDay().toInstant(UTC))).orElse(null),
            workingTime.isCurrent(),
            workingTime.validFrom().map(validFrom -> validFrom.isAfter(today)).orElse(false),
            workingTime.getMonday().hoursDoubleValue(),
            workingTime.getTuesday().hoursDoubleValue(),
            workingTime.getWednesday().hoursDoubleValue(),
            workingTime.getThursday().hoursDoubleValue(),
            workingTime.getFriday().hoursDoubleValue(),
            workingTime.getSaturday().hoursDoubleValue(),
            workingTime.getSunday().hoursDoubleValue()
        );
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
            workingTime.workdays().forEach((dayOfWeek, workDayDuration) -> {
                setter.get(dayOfWeek).accept(workDayDuration.hoursDoubleValue());
            });
        } else {
            // every day has the same hours
            // -> individual input fields should be empty
            // -> working time input should be set
            builder.workingTime(workingTime.workdays().get(MONDAY).hoursDoubleValue());
        }

        return builder
            .id(workingTime.id().value())
            .validFrom(workingTime.validFrom().orElse(null))
            .minValidFrom(workingTime.minValidFrom().orElse(null))
            .maxValidFrom(workingTime.validTo().orElse(null))
            .userId(workingTime.userIdComposite().localId().value())
            .workday(workingTime.actualWorkingDays())
            .build();
    }

    private WorkWeekUpdate dtoToWorkWeekUpdate(WorkingTimeDto workingTimeDto) {

        final WorkWeekUpdate.Builder builder = WorkWeekUpdate.builder()
            .validFrom(workingTimeDto.getValidFrom());

        if (workingTimeDto.isWorkDayMonday()) {
            builder.monday(requireNonNullElseGet(workingTimeDto.getWorkingTimeMonday(), workingTimeDto::getWorkingTime));
        }
        if (workingTimeDto.isWorkDayTuesday()) {
            builder.tuesday(requireNonNullElseGet(workingTimeDto.getWorkingTimeTuesday(), workingTimeDto::getWorkingTime));
        }
        if (workingTimeDto.isWorkDayWednesday()) {
            builder.wednesday(requireNonNullElseGet(workingTimeDto.getWorkingTimeWednesday(), workingTimeDto::getWorkingTime));
        }
        if (workingTimeDto.isWorkDayThursday()) {
            builder.thursday(requireNonNullElseGet(workingTimeDto.getWorkingTimeThursday(), workingTimeDto::getWorkingTime));
        }
        if (workingTimeDto.isWorkDayFriday()) {
            builder.friday(requireNonNullElseGet(workingTimeDto.getWorkingTimeFriday(), workingTimeDto::getWorkingTime));
        }
        if (workingTimeDto.isWorkDaySaturday()) {
            builder.saturday(requireNonNullElseGet(workingTimeDto.getWorkingTimeSaturday(), workingTimeDto::getWorkingTime));
        }
        if (workingTimeDto.isWorkDaySunday()) {
            builder.sunday(requireNonNullElseGet(workingTimeDto.getWorkingTimeSunday(), workingTimeDto::getWorkingTime));
        }

        return builder.build();
    }
}
