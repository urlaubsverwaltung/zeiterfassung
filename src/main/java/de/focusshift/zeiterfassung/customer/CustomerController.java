package de.focusshift.zeiterfassung.customer;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.USER_SEARCH_QUERY_PARAM;

@Controller
@RequestMapping("/settings/customers")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_SETTINGS_GLOBAL')")
class CustomerController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final UserSearchViewHelper userSearchViewHelper;

    CustomerController(CustomerService customerService, UserSearchViewHelper userSearchViewHelper) {
        this.customerService = customerService;
        this.userSearchViewHelper = userSearchViewHelper;
    }

    @GetMapping
    String list(Model model) {
        try {
            model.addAttribute("customers", customerService.findAll());
        } catch (Exception e) {
            LOG.error("Failed to load customers — {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            model.addAttribute("customers", java.util.List.of());
            model.addAttribute("loadError", e.getMessage());
        }
        return "settings/customers";
    }

    @PostMapping
    String create(@RequestParam String name) {
        customerService.create(name);
        return "redirect:/settings/customers";
    }

    @PostMapping("/{id}")
    String update(@PathVariable Long id,
                  @RequestParam String name,
                  @RequestParam(required = false) boolean active) {
        customerService.update(new CustomerId(id), name, active);
        return "redirect:/settings/customers";
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long id) {
        customerService.delete(new CustomerId(id));
        return "redirect:/settings/customers";
    }

    @GetMapping(params = USER_SEARCH_QUERY_PARAM, headers = TURBO_FRAME_HEADER)
    org.springframework.web.servlet.ModelAndView userSearchFragment(
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
