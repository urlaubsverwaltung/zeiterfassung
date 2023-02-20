package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.HALF_EVEN;
import static org.springframework.util.StringUtils.hasText;

@Controller
@RequestMapping("/users/{userId}/overtime-account")
class OvertimeAccountController implements HasLaunchpad, HasTimeClock {

    private final UserManagementService userManagementService;
    private final OvertimeAccountServiceImpl overtimeAccountService;

    OvertimeAccountController(UserManagementService userManagementService, OvertimeAccountServiceImpl overtimeAccountService) {
        this.userManagementService = userManagementService;
        this.overtimeAccountService = overtimeAccountService;
    }

    @GetMapping
    String get(@PathVariable("userId") Long userId, Model model,
               @RequestParam(value = "query", required = false, defaultValue = "") String query,
               @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame) {

        final UserLocalId userLocalId = new UserLocalId(userId);
        final OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(userLocalId);
        final OvertimeAccountDto overtimeAccountDto = toOvertimeAccountDto(overtimeAccount);

        prepareGetRequestModel(model, query, userId, overtimeAccountDto);

        if (hasText(turboFrame)) {
            return "usermanagement/users::#" + turboFrame;
        } else {
            return "usermanagement/users";
        }
    }

    @PostMapping
    String post(@PathVariable("userId") Long userId, Model model,
                @ModelAttribute("overtimeAccount") OvertimeAccountDto overtimeAccountDto, BindingResult result,
                @RequestParam(value = "query", required = false, defaultValue = "") String query,
                @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame) {

        if (result.hasErrors()) {
            prepareGetRequestModel(model, query, userId, overtimeAccountDto);
            if (hasText(turboFrame)) {
                return "usermanagement/users::#" + turboFrame;
            } else {
                return "usermanagement/users";
            }
        }

        overtimeAccountService.updateOvertimeAccount(toOvertimeAccount(userId, overtimeAccountDto));

        return "redirect:/users/%s/overtime-account".formatted(userId);
    }

    private void prepareGetRequestModel(Model model, String query, Long userId, OvertimeAccountDto overtimeAccountDto) {

        final List<UserDto> users = userManagementService.findAllUsers(query)
            .stream()
            .map(UserManagementController::userToDto)
            .toList();

        final UserLocalId userLocalId = new UserLocalId(userId);

        final UserDto selectedUser = users.stream()
            .filter(u -> u.id() == userId)
            .findFirst()
            .or(() -> userManagementService.findUserByLocalId(userLocalId).map(UserManagementController::userToDto))
            .orElseThrow(() -> new IllegalArgumentException("could not find person=%s".formatted(userId)));

        model.addAttribute("section", "overtime");
        model.addAttribute("query", query);
        model.addAttribute("users", users);
        model.addAttribute("selectedUser", selectedUser);
        model.addAttribute("personSearchFormAction", "/users/%s/overtime-account".formatted(selectedUser.id()));
        model.addAttribute("overtimeAccount", overtimeAccountDto);
    }

    private static OvertimeAccountDto toOvertimeAccountDto(OvertimeAccount overtimeAccount) {
        final OvertimeAccountDto dto = new OvertimeAccountDto();
        dto.setAllowed(overtimeAccount.isAllowed());
        dto.setMaxAllowedOvertime(overtimeAccount.getMaxAllowedOvertimeHours().map(BigDecimal::doubleValue).orElse(null));
        return dto;
    }

    private static OvertimeAccount toOvertimeAccount(Long userId, OvertimeAccountDto dto) {
        return new OvertimeAccount(new UserLocalId(userId), dto.isAllowed(), hoursToDuration(dto.getMaxAllowedOvertime()));
    }

    private static Duration hoursToDuration(Double hours) {
        if (hours == null) {
            return null;
        }
        final BigDecimal bigDecimal = BigDecimal.valueOf(hours);
        final int hoursPart = bigDecimal.setScale(0, DOWN).abs().intValue();
        final int minutesPart = bigDecimal.remainder(ONE).multiply(BigDecimal.valueOf(60)).setScale(0, HALF_EVEN).abs().intValueExact();
        return Duration.ofHours(hoursPart).plusMinutes(minutesPart);
    }
}
