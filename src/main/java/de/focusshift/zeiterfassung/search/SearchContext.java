package de.focusshift.zeiterfassung.search;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import jakarta.servlet.http.HttpServletRequest;

public interface SearchContext {

    /**
     * Returns the request URI path without contextPath.
     *
     * @return the request URI path without contextPath
     */
    default String getRequestPath() {
        return getRequestPath(getRequest());
    }

    /**
     * Returns the request URI path without contextPath.
     *
     * @param request the http request
     * @return the request URI path without contextPath
     */
    default String getRequestPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    HttpServletRequest getRequest();

    /**
     * Returns the user doing the search (currently logged-in user)
     *
     * @return the user doing the search (currently logged-in user)
     */
    CurrentOidcUser getUser();

    static SearchContext of(HttpServletRequest request, CurrentOidcUser user) {
        return new SearchContext() {
            @Override
            public HttpServletRequest getRequest() {
                return request;
            }

            @Override
            public CurrentOidcUser getUser() {
                return user;
            }
        };
    }
}
