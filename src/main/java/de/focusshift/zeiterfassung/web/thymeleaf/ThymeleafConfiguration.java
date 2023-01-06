package de.focusshift.zeiterfassung.web.thymeleaf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class ThymeleafConfiguration implements WebMvcConfigurer {

    private MessageSource messageSource;

    @Autowired
    void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addPrinter(new DurationPrinter(messageSource));
    }
}
