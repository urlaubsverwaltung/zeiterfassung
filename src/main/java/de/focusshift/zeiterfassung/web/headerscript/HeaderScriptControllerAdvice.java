package de.focusshift.zeiterfassung.web.headerscript;

import de.focusshift.zeiterfassung.web.DataProviderInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.web.servlet.ModelAndView;

class HeaderScriptControllerAdvice extends DataProviderInterceptor {

    private final HeaderScriptConfigProperties properties;

    HeaderScriptControllerAdvice(HeaderScriptConfigProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void addData(@NonNull ModelAndView modelAndView, @NonNull HttpServletRequest request) {
        modelAndView.getModelMap().addAttribute("headerScriptContent", properties.content());
    }
}
