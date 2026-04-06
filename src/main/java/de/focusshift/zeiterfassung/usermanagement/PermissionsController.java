package de.focusshift.zeiterfassung.usermanagement;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.security.SessionService;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.slf4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.FRAME_USERS_SUGGESTION;
import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.USER_SEARCH_QUERY_PARAM;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.usermanagement.UserManagementController.userToDto;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@Controller
@RequestMapping("/users/{userId}/permissions")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_PERMISSIONS_EDIT_ALL')")
class PermissionsController implements HasLaunchpad, HasTimeClock, HasUserSearch {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final UserManagementService userManagementService;
    private final SessionService sessionService;
    private final UserSearchViewHelper userSearchViewHelper;

    PermissionsController(UserManagementService userManagementService, SessionService sessionService,
                          UserSearchViewHelper userSearchViewHelper) {
        this.userManagementService = userManagementService;
        this.sessionService = sessionService;
        this.userSearchViewHelper = userSearchViewHelper;
    }

    @GetMapping
    ModelAndView get(@PathVariable Long userId, Model model,
                     @RequestParam(value = USER_SEARCH_QUERY_PARAM, required = false, defaultValue = "") String query,
                     @CurrentUser CurrentOidcUser currentUser) {

        prepareGetRequestModel(model, query, userId, this::userToPermissionsDto, currentUser);
        return new ModelAndView("usermanagement/users");
    }

    @GetMapping(params = USER_SEARCH_QUERY_PARAM, headers = TURBO_FRAME_HEADER)
    ModelAndView userSearchFragment(@RequestParam(USER_SEARCH_QUERY_PARAM) String query,
                                    @PathVariable(required = false) Long userId,
                                    @RequestHeader(TURBO_FRAME_HEADER) String turboFrame,
                                    @CurrentUser CurrentOidcUser currentUser, Model model) {

        if (FRAME_USERS_SUGGESTION.equals(turboFrame)) {
            return userSearchViewHelper.getSuggestionFragment(query, currentUser, model,
                suggestion -> "/users/%s/permissions".formatted(suggestion.userLocalId().value())
            );
        } else if ("person-frame".equals(turboFrame) && userId != null) {
            return get(userId, model, query, currentUser);
        } else {
            LOG.error("unknown turbo-frame requested or person-frame but without userId");
            return new ModelAndView("error/404", UNPROCESSABLE_CONTENT);
        }
    }

    @PostMapping
    String post(@PathVariable Long userId, Model model,
                @ModelAttribute("permissions") PermissionsDto permissionsDto,
                @RequestParam(value = USER_SEARCH_QUERY_PARAM, required = false, defaultValue = "") String query,
                @CurrentUser CurrentOidcUser currentUser) {

        final UserLocalId userLocalId = new UserLocalId(userId);

        try {
            final Set<SecurityRole> newPermissions = permissionsDtoToSecurityRoles(permissionsDto);
            userManagementService.updateUserPermissions(userLocalId, newPermissions);
            sessionService.markSessionToReloadAuthorities(userLocalId);
        } catch (UserNotFoundException e) {
            throw new IllegalArgumentException("could not find person=%s".formatted(userLocalId));
        }

        prepareGetRequestModel(model, query, userId, this::userToPermissionsDto, currentUser);

        return "redirect:/users/%s/permissions".formatted(userId);
    }

    private void prepareGetRequestModel(Model model, String query, Long selectedUserIdValue, Function<User, PermissionsDto> permissionsDtoSupplier, CurrentOidcUser currentUser) {

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
        model.addAttribute("permissions", permissionsDto);

        model.addAttribute("allowedToEditWorkingTime", currentUser.hasRole(ZEITERFASSUNG_WORKING_TIME_EDIT_ALL));
        model.addAttribute("allowedToEditOvertimeAccount", currentUser.hasRole(ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL));
        model.addAttribute("allowedToEditPermissions", currentUser.hasRole(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL));
        model.addAttribute("allowedToEditTimeentries", currentUser.hasRole(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL));
    }

    private PermissionsDto userToPermissionsDto(User user) {

        final PermissionsDto permissionsDto = new PermissionsDto();

        for (SecurityRole role : user.authorities()) {
            switch (role) {
                case ZEITERFASSUNG_VIEW_REPORT_ALL -> permissionsDto.setViewReportAll(true);
                case ZEITERFASSUNG_WORKING_TIME_EDIT_ALL -> permissionsDto.setWorkingTimeEditAll(true);
                case ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL, ZEITERFASSUNG_SETTINGS_GLOBAL ->
                    permissionsDto.setGlobalSettings(true);
                case ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL -> permissionsDto.setOvertimeEditAll(true);
                case ZEITERFASSUNG_PERMISSIONS_EDIT_ALL -> permissionsDto.setPermissionsEditAll(true);
                case ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL -> permissionsDto.setTimeEntryEditAll(true);
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

        for (SecurityRole role : SecurityRole.values()) {
            switch (role) {
                case ZEITERFASSUNG_VIEW_REPORT_ALL -> adder.accept(permissionsDto::isViewReportAll, role);
                case ZEITERFASSUNG_WORKING_TIME_EDIT_ALL -> adder.accept(permissionsDto::isWorkingTimeEditAll, role);
                case ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL, ZEITERFASSUNG_SETTINGS_GLOBAL ->
                    adder.accept(permissionsDto::isGlobalSettings, role);
                case ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL -> adder.accept(permissionsDto::isOvertimeEditAll, role);
                case ZEITERFASSUNG_PERMISSIONS_EDIT_ALL -> adder.accept(permissionsDto::isPermissionsEditAll, role);
                case ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL -> adder.accept(permissionsDto::isTimeEntryEditAll, role);
                case ZEITERFASSUNG_OPERATOR, ZEITERFASSUNG_USER -> { /* ok */ }
            }
        }

        return securityRoles;
    }
}
