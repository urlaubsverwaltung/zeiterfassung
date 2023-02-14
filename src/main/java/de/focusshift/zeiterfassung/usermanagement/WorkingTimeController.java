package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static de.focusshift.zeiterfassung.security.SecurityRules.ALLOW_EDIT_WORKING_TIME_ALL;
import static java.math.BigDecimal.ZERO;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

@Controller
@RequestMapping("/users/{userId}/working-time")
@PreAuthorize(ALLOW_EDIT_WORKING_TIME_ALL)
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
               @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame) {

        final List<UserDto> users = userManagementService.findAllUsers(query)
            .stream()
            .map(UserManagementController::userToDto)
            .toList();

        final UserDto selectedUser = users.stream().filter(u -> u.id() == userId)
            .findFirst()
            .or(() -> userManagementService.findUserByLocalId(new UserLocalId(userId)).map(UserManagementController::userToDto))
            .orElseThrow(() -> new IllegalArgumentException("could not find person=%s".formatted(userId)));

        final WorkingTime workingTime = workingTimeService.getWorkingTimeByUser(new UserLocalId(userId));
        final WorkingTimeDto workingTimeDto = workingTimeToDto(workingTime);

        model.addAttribute("query", query);
        model.addAttribute("users", users);
        model.addAttribute("selectedUser", selectedUser);
        model.addAttribute("workingTime", workingTimeDto);
        model.addAttribute("personSearchFormAction", "/users/" + selectedUser.id());

        if (StringUtils.hasText(turboFrame)) {
            return "usermanagement/users::#" + turboFrame;
        } else {
            return "usermanagement/users";
        }
    }

    @PostMapping
    String post(@PathVariable("userId") Long userId, Model model,
                @ModelAttribute("workingTime") WorkingTimeDto workingTimeDto, BindingResult result,
                @RequestParam(value = "query", required = false, defaultValue = "") String query,
                @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
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

            final List<UserDto> users = userManagementService.findAllUsers(query)
                .stream()
                .map(UserManagementController::userToDto)
                .toList();

            final UserDto selectedUser = users.stream().filter(u -> u.id() == userId)
                .findFirst()
                .or(() -> userManagementService.findUserByLocalId(new UserLocalId(userId)).map(UserManagementController::userToDto))
                .orElseThrow(() -> new IllegalArgumentException("could not find person=%s".formatted(userId)));

            model.addAttribute("query", query);
            model.addAttribute("users", users);
            model.addAttribute("selectedUser", selectedUser);
            model.addAttribute("workingTime", workingTimeDto);
            model.addAttribute("personSearchFormAction", "/users/" + selectedUser.id());

            if (StringUtils.hasText(turboFrame)) {
                return "usermanagement/users::#" + turboFrame;
            } else {
                return "usermanagement/users";
            }
        }

        final WorkingTime workingTime = dtoToWorkingTime(workingTimeDto);

        workingTimeService.updateWorkingTime(workingTime);

        return "redirect:/users/%s/working-time".formatted(userId);
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
            .userId(workingTime.getUserId().value())
            .workday(workingTime.getWorkingDays().stream().map(WorkDay::dayOfWeek).toList())
            .build();
    }

    private WorkingTime dtoToWorkingTime(WorkingTimeDto workingTimeDto) {

        final WorkingTime.Builder builder = WorkingTime.builder();

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

        return builder
            .userId(new UserLocalId(workingTimeDto.getUserId()))
            .build();
    }
}
