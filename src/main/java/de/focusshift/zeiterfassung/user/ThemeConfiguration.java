package de.focusshift.zeiterfassung.user;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class ThemeConfiguration implements WebMvcConfigurer {

    private final UserThemeDataProvider userThemeDataProvider;

    ThemeConfiguration(UserThemeDataProvider userThemeDataProvider) {
        this.userThemeDataProvider = userThemeDataProvider;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userThemeDataProvider);
    }
}
