package de.focusshift.zeiterfassung.activitytype;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.USER_SEARCH_QUERY_PARAM;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;

@Controller
@RequestMapping("/settings/activity-types")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_SETTINGS_GLOBAL')")
class ActivityTypeController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    private final ActivityTypeService activityTypeService;
    private final UserSearchViewHelper userSearchViewHelper;

    ActivityTypeController(ActivityTypeService activityTypeService, UserSearchViewHelper userSearchViewHelper) {
        this.activityTypeService = activityTypeService;
        this.userSearchViewHelper = userSearchViewHelper;
    }

    @GetMapping
    String list(Model model) {
        model.addAttribute("activityTypes", activityTypeService.findAll());
        return "settings/activity-types";
    }

    @PostMapping
    String create(@RequestParam String name) {
        activityTypeService.create(name);
        return "redirect:/settings/activity-types";
    }

    @PostMapping("/{id}")
    String update(@PathVariable Long id,
                  @RequestParam String name,
                  @RequestParam(required = false) boolean active) {
        activityTypeService.update(new ActivityTypeId(id), name, active);
        return "redirect:/settings/activity-types";
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long id) {
        activityTypeService.delete(new ActivityTypeId(id));
        return "redirect:/settings/activity-types";
    }

    @GetMapping(params = USER_SEARCH_QUERY_PARAM, headers = TURBO_FRAME_HEADER)
    ModelAndView userSearchFragment(
        @RequestParam(USER_SEARCH_QUERY_PARAM) String query,
        @CurrentUser CurrentOidcUser currentUser,
        Model model
    ) {
        return userSearchViewHelper.getSuggestionFragment(query, currentUser, model,
            suggestion -> {
                if (suggestion.userIdComposite().equals(currentUser.getUserIdComposite())) {
                    return "/timeentries";
                } else {
                    return "/timeentries/users/%s".formatted(suggestion.userLocalId().value());
                }
            }
        );
    }
}
