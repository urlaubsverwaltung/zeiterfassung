package de.focusshift.zeiterfassung.footer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

class FooterControllerAdvice implements HandlerInterceptor {

    private final String applicationVersion;

    FooterControllerAdvice(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {

        if (modelAndView != null && modelAndView.hasView() && !redirectOrForward(modelAndView)) {
            modelAndView.getModelMap().addAttribute("version", applicationVersion);
        }
    }

    private static boolean redirectOrForward(ModelAndView modelAndView) {
        final String viewName = modelAndView.getViewName();
        return viewName != null && (viewName.startsWith("redirect") || viewName.startsWith("forward"));
    }
}
