package de.focusshift.zeiterfassung.usermanagement;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.usermanagement.UserManagementController.hasAuthority;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static org.springframework.util.StringUtils.hasText;

@Controller
@RequestMapping("/users/{userId}/overtime-account")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL')")
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
               @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
               @CurrentSecurityContext SecurityContext securityContext) {

        final UserLocalId userLocalId = new UserLocalId(userId);
        final OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(userLocalId);
        final OvertimeAccountDto overtimeAccountDto = toOvertimeAccountDto(overtimeAccount);

        prepareGetRequestModel(model, query, userId, overtimeAccountDto, securityContext);

        if (hasText(turboFrame)) {
            return "usermanagement/users::#" + turboFrame;
        } else {
            return "usermanagement/users";
        }
    }

    @PostMapping
    ModelAndView post(@PathVariable("userId") Long userId, Model model,
                      @ModelAttribute("overtimeAccount") OvertimeAccountDto overtimeAccountDto,
                      @RequestParam(value = "query", required = false, defaultValue = "") String query) {

        final UserLocalId userLocalId = new UserLocalId(userId);
        final boolean allowed = overtimeAccountDto.isAllowed();

        overtimeAccountService.updateOvertimeAccount(userLocalId, allowed);

        return new ModelAndView("redirect:/users/%s/overtime-account".formatted(userId));
    }

    private void prepareGetRequestModel(Model model, String query, Long userId, OvertimeAccountDto overtimeAccountDto,
                                        SecurityContext securityContext) {

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
        model.addAttribute("slug", "overtime-account");
        model.addAttribute("users", users);
        model.addAttribute("selectedUser", selectedUser);
        model.addAttribute("personSearchFormAction", "/users/%s/overtime-account".formatted(selectedUser.id()));
        model.addAttribute("overtimeAccount", overtimeAccountDto);

        model.addAttribute("allowedToEditWorkingTime", hasAuthority(ZEITERFASSUNG_WORKING_TIME_EDIT_ALL, securityContext));
        model.addAttribute("allowedToEditOvertimeAccount", hasAuthority(ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL, securityContext));
        model.addAttribute("allowedToEditPermissions", hasAuthority(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL, securityContext));
    }

    private static OvertimeAccountDto toOvertimeAccountDto(OvertimeAccount overtimeAccount) {
        final OvertimeAccountDto dto = new OvertimeAccountDto();
        dto.setAllowed(overtimeAccount.isAllowed());
        return dto;
    }
}
