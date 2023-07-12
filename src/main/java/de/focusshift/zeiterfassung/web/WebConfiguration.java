package de.focusshift.zeiterfassung.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfiguration implements WebMvcConfigurer {

    private final AuthoritiesModelProvider authoritiesModelProvider;
    private final DoubleFormatter doubleFormatter;
    private final CurrentTenantInterceptor currentTenantInterceptor;

    WebConfiguration(AuthoritiesModelProvider authoritiesModelProvider, CurrentTenantInterceptor currentTenantInterceptor, DoubleFormatter doubleFormatter) {
        this.authoritiesModelProvider = authoritiesModelProvider;
        this.currentTenantInterceptor = currentTenantInterceptor;
        this.doubleFormatter = doubleFormatter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authoritiesModelProvider);
        registry.addInterceptor(currentTenantInterceptor);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(doubleFormatter);
    }
}
