package de.focusshift.zeiterfassung.projecttype;

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
@RequestMapping("/settings/project-types")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_SETTINGS_GLOBAL')")
class ProjectTypeController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    private final ProjectTypeService projectTypeService;
    private final UserSearchViewHelper userSearchViewHelper;

    ProjectTypeController(ProjectTypeService projectTypeService, UserSearchViewHelper userSearchViewHelper) {
        this.projectTypeService = projectTypeService;
        this.userSearchViewHelper = userSearchViewHelper;
    }

    @GetMapping
    String list(Model model) {
        model.addAttribute("projectTypes", projectTypeService.findAll());
        return "settings/project-types";
    }

    @PostMapping
    String create(@RequestParam String name) {
        projectTypeService.create(name);
        return "redirect:/settings/project-types";
    }

    @PostMapping("/{id}")
    String update(@PathVariable Long id,
                  @RequestParam String name,
                  @RequestParam(required = false) boolean active) {
        projectTypeService.update(new ProjectTypeId(id), name, active);
        return "redirect:/settings/project-types";
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long id) {
        projectTypeService.delete(new ProjectTypeId(id));
        return "redirect:/settings/project-types";
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
