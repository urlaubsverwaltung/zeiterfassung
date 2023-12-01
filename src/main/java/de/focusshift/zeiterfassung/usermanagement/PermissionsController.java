package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRole;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.usermanagement.UserManagementController.hasAuthority;
import static de.focusshift.zeiterfassung.usermanagement.UserManagementController.userToDto;

@Controller
@RequestMapping("/users/{userId}/permissions")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_PERMISSIONS_EDIT_ALL')")
class PermissionsController {

    private final UserManagementService userManagementService;

    PermissionsController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    String get(@PathVariable("userId") Long userId, Model model,
               @RequestParam(value = "query", required = false, defaultValue = "") String query,
               @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame,
               @CurrentSecurityContext SecurityContext securityContext) {

        prepareGetRequestModel(model, query, userId, this::userToPermissionsDto, securityContext);

        if ("person-frame".equals(turboFrame)) {
            return "usermanagement/users::#person-frame";
        } else if ("person-list-frame".equals(turboFrame)) {
            return "usermanagement/users::#person-list-frame";
        } else {
            return "usermanagement/users";
        }
    }

    @PostMapping
    String post(@PathVariable("userId") Long userId, Model model,
                @ModelAttribute("permissions") PermissionsDto permissionsDto,
                @RequestParam(value = "query", required = false, defaultValue = "") String query,
                @CurrentSecurityContext SecurityContext securityContext) {

        final UserLocalId userLocalId = new UserLocalId(userId);

        try {
            final Set<SecurityRole> newPermissions = permissionsDtoToSecurityRoles(permissionsDto);
            userManagementService.updateUserPermissions(userLocalId, newPermissions);
        } catch (UserNotFoundException e) {
            throw new IllegalArgumentException("could not find person=%s".formatted(userLocalId));
        }

        prepareGetRequestModel(model, query, userId, this::userToPermissionsDto, securityContext);

        return "redirect:/users/%s/permissions".formatted(userId);
    }

    private void prepareGetRequestModel(Model model, String query, Long selectedUserIdValue, Function<User, PermissionsDto> permissionsDtoSupplier, SecurityContext securityContext) {

        final UserLocalId selectedUserId = new UserLocalId(selectedUserIdValue);
        final List<User> allUsers = userManagementService.findAllUsers(query);

        final User selectedUser = allUsers.stream()
            .filter(u -> u.userLocalId().value().equals(selectedUserIdValue))
            .findFirst()
            .or(() -> userManagementService.findUserByLocalId(selectedUserId))
            .orElseThrow(() -> new IllegalArgumentException("could not find person=%s".formatted(selectedUserId)));

        final UserDto selectedUserDto = userToDto(selectedUser);
        final List<UserDto> allUserDtos = allUsers
            .stream()
            .map(UserManagementController::userToDto)
            .toList();

        final PermissionsDto permissionsDto = permissionsDtoSupplier.apply(selectedUser);

        model.addAttribute("section", "permissions");
        model.addAttribute("query", query);
        model.addAttribute("slug", "permissions");
        model.addAttribute("users", allUserDtos);
        model.addAttribute("selectedUser", selectedUserDto);
        model.addAttribute("personSearchFormAction", "/users/%s/permissions".formatted(selectedUserDto.id()));
        model.addAttribute("permissions", permissionsDto);

        model.addAttribute("allowedToEditWorkingTime", hasAuthority(ZEITERFASSUNG_WORKING_TIME_EDIT_ALL, securityContext));
        model.addAttribute("allowedToEditOvertimeAccount", hasAuthority(ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL, securityContext));
        model.addAttribute("allowedToEditPermissions", hasAuthority(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL, securityContext));
    }

    private PermissionsDto userToPermissionsDto(User user) {

        final PermissionsDto permissionsDto = new PermissionsDto();

        for (SecurityRole role : user.authorities()) {
            switch (role) {
                case ZEITERFASSUNG_VIEW_REPORT_ALL -> permissionsDto.setViewReportAll(true);
                case ZEITERFASSUNG_WORKING_TIME_EDIT_ALL -> permissionsDto.setWorkingTimeEditAll(true);
                case ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL -> permissionsDto.setOvertimeEditAll(true);
                case ZEITERFASSUNG_PERMISSIONS_EDIT_ALL -> permissionsDto.setPermissionsEditAll(true);
                case ZEITERFASSUNG_OPERATOR, ZEITERFASSUNG_USER -> { /* ok */ }
            }
        }

        return permissionsDto;
    }

    private Set<SecurityRole> permissionsDtoToSecurityRoles(PermissionsDto permissionsDto) {

        final Set<SecurityRole> securityRoles = new HashSet<>();
        final BiConsumer<BooleanSupplier, SecurityRole> adder = (isChecked, role) -> {
            if (isChecked.getAsBoolean()) {
                securityRoles.add(role);
            }
        };

        for(SecurityRole role : SecurityRole.values()) {
            switch (role) {
                case ZEITERFASSUNG_VIEW_REPORT_ALL -> adder.accept(permissionsDto::isViewReportAll, role);
                case ZEITERFASSUNG_WORKING_TIME_EDIT_ALL -> adder.accept(permissionsDto::isWorkingTimeEditAll, role);
                case ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL -> adder.accept(permissionsDto::isOvertimeEditAll, role);
                case ZEITERFASSUNG_PERMISSIONS_EDIT_ALL -> adder.accept(permissionsDto::isPermissionsEditAll, role);
                case ZEITERFASSUNG_OPERATOR, ZEITERFASSUNG_USER -> { /* ok */ }
            }
        }

        return securityRoles;
    }
}
