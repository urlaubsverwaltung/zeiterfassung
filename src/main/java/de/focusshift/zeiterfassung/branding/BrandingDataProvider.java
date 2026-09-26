package de.focusshift.zeiterfassung.branding;

import de.focusshift.zeiterfassung.web.DataProviderInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides the configured application name to every rendered view.
 */
class BrandingDataProvider extends DataProviderInterceptor {

    private final BrandingConfigProperties brandingConfigProperties;

    BrandingDataProvider(BrandingConfigProperties brandingConfigProperties) {
        this.brandingConfigProperties = brandingConfigProperties;
    }

    @Override
    protected void addData(@NonNull ModelAndView modelAndView, @NonNull HttpServletRequest request) {
        modelAndView.getModelMap().addAttribute("applicationName", brandingConfigProperties.name());
    }
}
