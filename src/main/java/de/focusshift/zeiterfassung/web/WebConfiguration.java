package de.focusshift.zeiterfassung.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfiguration implements WebMvcConfigurer {

    private final AuthoritiesModelProvider authoritiesModelProvider;

    WebConfiguration(AuthoritiesModelProvider authoritiesModelProvider) {
        this.authoritiesModelProvider = authoritiesModelProvider;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authoritiesModelProvider);
    }
}
