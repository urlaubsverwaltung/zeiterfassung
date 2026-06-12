package de.focusshift.zeiterfassung.search;

import de.focusshift.zeiterfassung.security.AuthenticationFacade;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import static de.focusshift.zeiterfassung.search.UserSearchSuggestionsProvider.USER_SEARCH_QUERY_PARAM;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;

/**
 * Cross-cutting mechanism of the global person search.
 *
 * <p>
 * For every page rendered by a {@link HasUserSearch} controller this enables the search box ({@link #postHandle}). The
 * suggestions themselves are served by a dedicated turbo-frame request that targets the {@value #USER_SEARCH_TURBO_FRAME}
 * frame: such a request is short-circuited in {@link #preHandle} and answered with only the person-search fragment, so
 * the feature controller's (potentially expensive) handler is never invoked for a search-only request.
 *
 * <p>
 * Global search is only supported with enabled JavaScript: without Turbo the {@value #USER_SEARCH_TURBO_FRAME} frame
 * request is never issued.
 *
 * <p>
 * Authorization of the page is the page's own responsibility: a {@value #USER_SEARCH_TURBO_FRAME} request is still
 * routed to the feature controller's handler method and thus guarded by its existing security rules before this
 * interceptor short-circuits it. The visible suggestions are additionally scoped by
 * {@link UserSearchSuggestionsProvider#userSuggestions(CurrentOidcUser, String, Function)} to what the logged-in person may see.
 */
@Component
class UserSearchInterceptor implements HandlerInterceptor {

    /**
     * Turbo-Frame id of the suggestions frame, see {@code fragments/user-search.html}. A turbo-frame request carrying
     * this id is a search-only request.
     */
    static final String USER_SEARCH_TURBO_FRAME = "frame-users-suggestions";

    private final AuthenticationFacade authenticationFacade;
    private final UserSearchSuggestionsProvider userSearchSuggestionsProvider;
    private final ThymeleafViewResolver thymeleafViewResolver;

    UserSearchInterceptor(
        AuthenticationFacade authenticationFacade,
        UserSearchSuggestionsProvider userSearchSuggestionsProvider,
        ThymeleafViewResolver thymeleafViewResolver
    ) {
        this.authenticationFacade = authenticationFacade;
        this.userSearchSuggestionsProvider = userSearchSuggestionsProvider;
        this.thymeleafViewResolver = thymeleafViewResolver;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod) || !(handlerMethod.getBean() instanceof HasUserSearch userSearch)) {
            return true;
        }

        // only a search-only request (suggestions frame) is short-circuited; everything else runs the regular handler
        if (!USER_SEARCH_TURBO_FRAME.equals(request.getHeader(TURBO_FRAME_HEADER))) {
            return true;
        }

        final CurrentOidcUser user = authenticationFacade.getCurrentUser(request);
        final String query = request.getParameter(USER_SEARCH_QUERY_PARAM);
        final boolean allowedToSearch = isAllowedToSearch(user);

        final Map<String, Object> model = new HashMap<>();
        model.put("userSearchEnabled", allowedToSearch);
        model.put("userSearchQuery", query);

        if (allowedToSearch) {
            final UserSuggestionUrlStrategy urlStrategy = userSearch.userSuggestionUrlStrategy();
            final SearchContext context = SearchContext.of(request, user);
            final List<UserSuggestion> suggestions = userSearchSuggestionsProvider.userSuggestions(user, query,
                suggestion -> urlStrategy.buildSuggestionMainLink(suggestion, context));
            model.put("userSearchSuggestions", suggestions);
        } else {
            model.put("userSearchSuggestions", List.of());
        }

        final String viewName = userSearch.userSearchUiFragmentSupplier().get();
        renderPersonSearchFragment(viewName, model, request, response);

        // handler (and the rest of the dispatch) is skipped, the response is fully rendered above
        return false;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) {

        if (modelAndView == null || !(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        if (!(handlerMethod.getBean() instanceof HasUserSearch userSearch)) {
            return;
        }

        final String viewName = modelAndView.getViewName();
        if (viewName != null && viewName.startsWith("redirect:")) {
            return;
        }

        final CurrentOidcUser user = authenticationFacade.getCurrentUser(request);

        // only the cheap search-box attributes; suggestions are served by the short-circuited preHandle frame request
        modelAndView.addObject("userSearchEnabled", isAllowedToSearch(user));
        modelAndView.addObject("userSearchQuery", request.getParameter(USER_SEARCH_QUERY_PARAM));
        modelAndView.addObject("userSearchFragment", userSearch.userSearchUiFragmentSupplier().get());
    }

    private static boolean isAllowedToSearch(CurrentOidcUser user) {
        return user.hasAnyRole(
            ZEITERFASSUNG_VIEW_REPORT_ALL,
            ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL,
            ZEITERFASSUNG_WORKING_TIME_EDIT_ALL,
            ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL,
            ZEITERFASSUNG_PERMISSIONS_EDIT_ALL
        );
    }

    private void renderPersonSearchFragment(String viewName, Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        final Locale locale = RequestContextUtils.getLocale(request);
        final View view = thymeleafViewResolver.resolveViewName(viewName, locale);
        if (view == null) {
            throw new IllegalStateException("could not resolve person search view '%s'".formatted(viewName));
        }
        view.render(model, request, response);
    }
}
