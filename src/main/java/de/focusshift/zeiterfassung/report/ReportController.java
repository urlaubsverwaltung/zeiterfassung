package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.USER_SEARCH_QUERY_PARAM;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;

@Controller
@RequestMapping("/report")
class ReportController implements HasUserSearch {

    private final UserSearchViewHelper userSearchViewHelper;

    ReportController(UserSearchViewHelper userSearchViewHelper) {
        this.userSearchViewHelper = userSearchViewHelper;
    }

    @GetMapping
    public String userReport(RedirectAttributes redirectAttributes, HttpServletRequest request) {
        redirectAttributes.mergeAttributes(request.getParameterMap());
        return "forward:/report/week";
    }

    @GetMapping(params = USER_SEARCH_QUERY_PARAM, headers = TURBO_FRAME_HEADER)
    @PreAuthorize("hasAuthority('ZEITERFASSUNG_VIEW_REPORT_ALL')")
    ModelAndView userSearchFragment(@RequestParam(USER_SEARCH_QUERY_PARAM) String query, @CurrentUser CurrentOidcUser currentUser, Model model) {
        return userSearchViewHelper.getSuggestionFragment(query, currentUser, model,
            suggestion -> "/report/week?user=%s".formatted(suggestion.userLocalId().value())
        );
    }
}
