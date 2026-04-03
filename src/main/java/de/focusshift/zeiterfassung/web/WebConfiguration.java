package de.focusshift.zeiterfassung.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfiguration implements WebMvcConfigurer {

    private final DoubleFormatter doubleFormatter;
    private final CurrentTenantInterceptor currentTenantInterceptor;

    WebConfiguration(CurrentTenantInterceptor currentTenantInterceptor, DoubleFormatter doubleFormatter) {
        this.currentTenantInterceptor = currentTenantInterceptor;
        this.doubleFormatter = doubleFormatter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(currentTenantInterceptor);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(doubleFormatter);
    }
}
