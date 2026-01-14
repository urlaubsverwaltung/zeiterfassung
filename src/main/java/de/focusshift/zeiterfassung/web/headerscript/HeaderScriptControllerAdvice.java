package de.focusshift.zeiterfassung.web.headerscript;

import de.focusshift.zeiterfassung.web.DataProviderInterceptor;
import org.springframework.web.servlet.ModelAndView;

class HeaderScriptControllerAdvice extends DataProviderInterceptor {

    private final HeaderScriptConfigProperties properties;

    HeaderScriptControllerAdvice(HeaderScriptConfigProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void addData(ModelAndView modelAndView) {
        modelAndView.getModelMap().addAttribute("headerScriptContent", properties.content());
    }
}
