package de.focusshift.zeiterfassung;

import com.connect_group.thymeleaf_extras.ThymeleafExtrasDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ThymeleafConfig {

    @Bean
    ThymeleafExtrasDialect thymeleafExtrasDialect() {
        return new ThymeleafExtrasDialect();
    }
}
