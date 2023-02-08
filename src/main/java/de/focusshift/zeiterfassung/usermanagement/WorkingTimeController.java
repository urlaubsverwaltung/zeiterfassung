package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import jakarta.validation.Valid;
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
import java.util.Optional;

@Controller
@RequestMapping("/users/{userId}/working-time")
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
            .or(() -> userManagementService.findUserById(new UserLocalId(userId)).map(UserManagementController::userToDto))
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
                @Valid @ModelAttribute("workingTime") WorkingTimeDto workingTimeDto, BindingResult result,
                @RequestParam(value = "query", required = false, defaultValue = "") String query,
                @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame) {

        validator.validate(workingTimeDto, result);
        if (result.hasErrors()) {

            final List<UserDto> users = userManagementService.findAllUsers(query)
                .stream()
                .map(UserManagementController::userToDto)
                .toList();

            final UserDto selectedUser = users.stream().filter(u -> u.id() == userId)
                .findFirst()
                .or(() -> userManagementService.findUserById(new UserLocalId(userId)).map(UserManagementController::userToDto))
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

    private static WorkingTimeDto workingTimeToDto(WorkingTime workingTime) {

        final Optional<BigDecimal> workingHours = workingTime.getWorkingHours();
        final List<DayOfWeek> workingDays = workingTime.getWorkingDays().stream().map(WorkDay::dayOfWeek).toList();

        final WorkingTimeDto workingTimeDto;

        if (workingHours.isEmpty()) {
            workingTimeDto = WorkingTimeDto.builder()
                .userId(workingTime.getUserId().value())
                .workday(workingDays)
                .workingTimeMonday(workingTime.getMonday().hours())
                .workingTimeTuesday(workingTime.getTuesday().hours())
                .workingTimeWednesday(workingTime.getWednesday().hours())
                .workingTimeThursday(workingTime.getThursday().hours())
                .workingTimeFriday(workingTime.getFriday().hours())
                .workingTimeSaturday(workingTime.getSaturday().hours())
                .workingTimeSunday(workingTime.getSunday().hours())
                .build();
        } else {
            workingTimeDto = WorkingTimeDto.builder()
                .userId(workingTime.getUserId().value())
                .workday(workingDays)
                .workingTime(workingHours.get())
                .build();
        }

        return workingTimeDto;
    }

    private static WorkingTime dtoToWorkingTime(WorkingTimeDto workingTimeDto) {

        final WorkingTime.Builder builder;
        final Optional<BigDecimal> workingTime = Optional.ofNullable(workingTimeDto.getWorkingTime());

        if (workingTime.isPresent()) {
            final List<DayOfWeek> workdays = workingTimeDto.getWorkday().stream().map(WorkingTimeController::toDayOfWeek).toList();
            builder = WorkingTime.builder().workdays(workdays, workingTime.get());
        } else {
            builder = WorkingTime.builder()
                .monday(workingTimeDto.getWorkingTimeMonday())
                .tuesday(workingTimeDto.getWorkingTimeTuesday())
                .wednesday(workingTimeDto.getWorkingTimeWednesday())
                .thursday(workingTimeDto.getWorkingTimeThursday())
                .friday(workingTimeDto.getWorkingTimeFriday())
                .saturday(workingTimeDto.getWorkingTimeSaturday())
                .sunday(workingTimeDto.getWorkingTimeSunday());
        }

        return builder.userId(new UserLocalId(workingTimeDto.getUserId())).build();
    }

    private static DayOfWeek toDayOfWeek(String name) {
        return DayOfWeek.valueOf(name.toUpperCase());
    }
}
