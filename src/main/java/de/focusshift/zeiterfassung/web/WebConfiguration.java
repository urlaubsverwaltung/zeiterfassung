package de.focusshift.zeiterfassung.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfiguration implements WebMvcConfigurer {

    private final AuthoritiesModelProvider authoritiesModelProvider;
    private final DoubleFormatter doubleFormatter;

    WebConfiguration(AuthoritiesModelProvider authoritiesModelProvider, DoubleFormatter doubleFormatter) {
        this.authoritiesModelProvider = authoritiesModelProvider;
        this.doubleFormatter = doubleFormatter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authoritiesModelProvider);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(doubleFormatter);
    }
}
