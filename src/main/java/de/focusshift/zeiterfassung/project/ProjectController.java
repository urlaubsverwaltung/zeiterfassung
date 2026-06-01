package de.focusshift.zeiterfassung.project;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.customer.CustomerId;
import de.focusshift.zeiterfassung.customer.CustomerService;
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
@RequestMapping("/settings/customers/{customerId}/projects")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_SETTINGS_GLOBAL')")
class ProjectController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    private final ProjectService projectService;
    private final CustomerService customerService;
    private final UserSearchViewHelper userSearchViewHelper;

    ProjectController(ProjectService projectService, CustomerService customerService, UserSearchViewHelper userSearchViewHelper) {
        this.projectService = projectService;
        this.customerService = customerService;
        this.userSearchViewHelper = userSearchViewHelper;
    }

    @GetMapping
    String list(@PathVariable Long customerId, Model model) {
        final CustomerId customerIdObj = new CustomerId(customerId);
        model.addAttribute("customer", customerService.findById(customerIdObj)
            .orElseThrow(() -> new IllegalStateException("could not find customer id=%s".formatted(customerId))));
        model.addAttribute("projects", projectService.findAllByCustomer(customerIdObj));
        return "settings/projects";
    }

    @PostMapping
    String create(@PathVariable Long customerId, @RequestParam String name) {
        projectService.create(new CustomerId(customerId), name);
        return "redirect:/settings/customers/%d/projects".formatted(customerId);
    }

    @PostMapping("/{id}")
    String update(@PathVariable Long customerId,
                  @PathVariable Long id,
                  @RequestParam String name,
                  @RequestParam(required = false) boolean active) {
        projectService.update(new ProjectId(id), name, active);
        return "redirect:/settings/customers/%d/projects".formatted(customerId);
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long customerId, @PathVariable Long id) {
        projectService.delete(new ProjectId(id));
        return "redirect:/settings/customers/%d/projects".formatted(customerId);
    }

    @GetMapping(params = USER_SEARCH_QUERY_PARAM, headers = TURBO_FRAME_HEADER)
    ModelAndView userSearchFragment(
        @PathVariable Long customerId,
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
