package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.web.DataProviderInterface;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
public class UserThemeDataProvider implements DataProviderInterface {

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) {
        if (addDataIf(modelAndView)) {

            final Theme theme = Theme.SYSTEM;
            final String themeValueLowerCase = theme.name().toLowerCase();

            modelAndView.addObject("theme", themeValueLowerCase);
        }
    }
}
