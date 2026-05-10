package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.web.DataProviderInterface;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import java.util.Locale;

@Component
class LocaleModelInterceptor implements DataProviderInterface {

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) {
        if (addDataIf(modelAndView)) {
            final Locale currentLocale = LocaleContextHolder.getLocale();
            modelAndView.addObject("locale", currentLocale);
            modelAndView.addObject("language", currentLocale.toLanguageTag());
        }
    }
}
