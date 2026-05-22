package de.focusshift.zeiterfassung.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Order(1)
class CurrentTenantInterceptorWebConfig implements WebMvcConfigurer {

    private final CurrentTenantInterceptor currentTenantInterceptor;

    CurrentTenantInterceptorWebConfig(CurrentTenantInterceptor currentTenantInterceptor) {
        this.currentTenantInterceptor = currentTenantInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(currentTenantInterceptor);
    }
}
