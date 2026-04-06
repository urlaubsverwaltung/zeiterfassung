package de.focusshift.zeiterfassung.search;

import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.USER_SEARCH_QUERY_PARAM;
import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.isAllowedToSearch;

@ControllerAdvice(assignableTypes = HasUserSearch.class)
class UserSearchControllerAdvice {

    @ModelAttribute
    public void addAttributes(@RequestParam(value = USER_SEARCH_QUERY_PARAM, required = false) String userSearchQuery, Model model, @CurrentUser CurrentOidcUser user) {

        model.addAttribute("userSearchQuery", userSearchQuery);
        model.addAttribute("userSearchEnabled", isAllowedToSearch(user));
    }
}
