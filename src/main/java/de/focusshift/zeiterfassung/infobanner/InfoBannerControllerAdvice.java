package de.focusshift.zeiterfassung.infobanner;

import de.focusshift.zeiterfassung.web.DataProviderInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.web.servlet.ModelAndView;

class InfoBannerControllerAdvice extends DataProviderInterceptor {

    private final InfoBannerConfigProperties properties;

    InfoBannerControllerAdvice(InfoBannerConfigProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void addData(@NonNull ModelAndView modelAndView, @NonNull HttpServletRequest request) {
        modelAndView.getModelMap().addAttribute("infoBannerText", properties.text().de());
    }
}
