package de.focusshift.zeiterfassung.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfiguration implements WebMvcConfigurer {

    private final DoubleFormatter doubleFormatter;

    WebConfiguration(DoubleFormatter doubleFormatter) {
        this.doubleFormatter = doubleFormatter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(doubleFormatter);
    }
}
