package de.focusshift.zeiterfassung.usermanagement;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.slf4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.time.Duration;
import java.util.List;

import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.FRAME_USERS_SUGGESTION;
import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.USER_SEARCH_QUERY_PARAM;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static java.lang.invoke.MethodHandles.lookup;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.HALF_EVEN;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static org.springframework.util.StringUtils.hasText;

@Controller
@RequestMapping("/users/{userId}/overtime-account")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL')")
class OvertimeAccountController implements HasLaunchpad, HasTimeClock, HasUserSearch {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final UserManagementService userManagementService;
    private final OvertimeAccountService overtimeAccountService;
    private final UserSearchViewHelper userSearchViewHelper;

    OvertimeAccountController(UserManagementService userManagementService,
                              OvertimeAccountService overtimeAccountService,
                              UserSearchViewHelper userSearchViewHelper) {
        this.userManagementService = userManagementService;
        this.overtimeAccountService = overtimeAccountService;
        this.userSearchViewHelper = userSearchViewHelper;
    }

    @GetMapping
    ModelAndView get(@PathVariable Long userId, Model model,
                     @RequestParam(value = "query", required = false, defaultValue = "") String query,
                     @CurrentUser CurrentOidcUser currentUser) {

        final UserLocalId userLocalId = new UserLocalId(userId);
        final OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(userLocalId);
        final OvertimeAccountDto overtimeAccountDto = toOvertimeAccountDto(overtimeAccount);

        prepareGetRequestModel(model, query, userId, overtimeAccountDto, currentUser);
        return new ModelAndView("usermanagement/users");
    }

    @PostMapping
    ModelAndView post(@PathVariable Long userId, Model model,
                      @ModelAttribute("overtimeAccount") OvertimeAccountDto overtimeAccountDto, BindingResult result,
                      @RequestParam(value = "query", required = false, defaultValue = "") String query,
                      @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame,
                      @CurrentUser CurrentOidcUser currentUser) {

        if (result.hasErrors()) {
            prepareGetRequestModel(model, query, userId, overtimeAccountDto, currentUser);
            if (hasText(turboFrame)) {
                return new ModelAndView("usermanagement/users::#" + turboFrame, UNPROCESSABLE_CONTENT);
            } else {
                return new ModelAndView("usermanagement/users");
            }
        }

        final UserLocalId userLocalId = new UserLocalId(userId);
        final boolean allowed = overtimeAccountDto.isAllowed();
        final Duration maxAllowedOvertime = hoursToDuration(overtimeAccountDto.getMaxAllowedOvertime());

        overtimeAccountService.updateOvertimeAccount(userLocalId, allowed, maxAllowedOvertime);

        return new ModelAndView("redirect:/users/%s/overtime-account".formatted(userId));
    }

    @GetMapping(params = USER_SEARCH_QUERY_PARAM, headers = TURBO_FRAME_HEADER)
    ModelAndView userSearchFragment(@RequestParam(USER_SEARCH_QUERY_PARAM) String query,
                                    @PathVariable(required = false) Long userId,
                                    @RequestHeader(TURBO_FRAME_HEADER) String turboFrame,
                                    @CurrentUser CurrentOidcUser currentUser, Model model) {

        if (FRAME_USERS_SUGGESTION.equals(turboFrame)) {
            return userSearchViewHelper.getSuggestionFragment(query, currentUser, model,
                suggestion -> "/users/%s/overtime-account".formatted(suggestion.userLocalId().value())
            );
        } else if ("person-frame".equals(turboFrame) && userId != null) {
            return get(userId, model, query, currentUser);
        } else {
            LOG.error("unknown turbo-frame requested or person-frame but without userId");
            return new ModelAndView("error/404", UNPROCESSABLE_CONTENT);
        }
    }

    private void prepareGetRequestModel(Model model, String query, Long userId, OvertimeAccountDto overtimeAccountDto,
                                        CurrentOidcUser currentUser) {

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
        model.addAttribute("overtimeAccount", overtimeAccountDto);

        model.addAttribute("allowedToEditWorkingTime", currentUser.hasRole(ZEITERFASSUNG_WORKING_TIME_EDIT_ALL));
        model.addAttribute("allowedToEditOvertimeAccount", currentUser.hasRole(ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL));
        model.addAttribute("allowedToEditPermissions", currentUser.hasRole(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL));
    }

    private static OvertimeAccountDto toOvertimeAccountDto(OvertimeAccount overtimeAccount) {
        final OvertimeAccountDto dto = new OvertimeAccountDto();
        dto.setAllowed(overtimeAccount.isAllowed());
        dto.setMaxAllowedOvertime(overtimeAccount.getMaxAllowedOvertimeHours().map(BigDecimal::doubleValue).orElse(null));
        return dto;
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
