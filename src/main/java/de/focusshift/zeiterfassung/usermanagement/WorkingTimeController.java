package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/users/{userId}/working-time")
class WorkingTimeController implements HasTimeClock, HasLaunchpad {

    private final UserManagementService userManagementService;
    private final WorkingTimeService workingTimeService;

    WorkingTimeController(UserManagementService userManagementService, WorkingTimeService workingTimeService) {
        this.userManagementService = userManagementService;
        this.workingTimeService = workingTimeService;
    }

    @GetMapping
    String get(@PathVariable("userId") Long userId, Model model) {

        List<UserDto> users = userManagementService.findAllUsers()
            .stream()
            .map(UserManagementController::userToDto)
            .toList();

        final WorkingTime workingTime = workingTimeService.getWorkingTimeByUser(new UserLocalId(userId));
        final WorkingTimeDto workingTimeDto = workingTimeToDto(workingTime);

        model.addAttribute("users", users);
        model.addAttribute("selectedUser", users.stream().filter(u -> u.id() == userId).findFirst().orElse(null));
        model.addAttribute("workingTime", workingTimeDto);

        return "usermanagement/users";
    }

    @PostMapping
    String post(@PathVariable("userId") Long userId, @ModelAttribute("workingTime") WorkingTimeDto workingTimeDto) {

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
