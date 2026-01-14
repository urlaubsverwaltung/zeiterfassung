package de.focusshift.zeiterfassung.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Base class for interceptors that provide data to the model for all views.
 */
public abstract class DataProviderInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        if (modelAndView != null && modelAndView.hasView() && !redirectOrForward(modelAndView)) {
            addData(modelAndView);
        }
    }

    /**
     * Add data to the model. Called for all non-redirect/forward views.
     *
     * @param modelAndView the model and view to add data to
     */
    protected abstract void addData(ModelAndView modelAndView);

    private static boolean redirectOrForward(ModelAndView modelAndView) {
        final String viewName = modelAndView.getViewName();
        return viewName != null && (viewName.startsWith("redirect") || viewName.startsWith("forward"));
    }
}
