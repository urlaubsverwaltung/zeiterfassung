package de.focusshift.zeiterfassung.infobanner;

import de.focusshift.zeiterfassung.web.DataProviderInterceptor;
import org.springframework.web.servlet.ModelAndView;

class InfoBannerControllerAdvice extends DataProviderInterceptor {

    private final InfoBannerConfigProperties properties;

    InfoBannerControllerAdvice(InfoBannerConfigProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void addData(ModelAndView modelAndView) {
        modelAndView.getModelMap().addAttribute("infoBannerText", properties.text().de());
    }
}
