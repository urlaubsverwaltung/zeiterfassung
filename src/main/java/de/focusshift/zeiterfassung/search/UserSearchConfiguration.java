package de.focusshift.zeiterfassung.search;

import de.focusshift.zeiterfassung.timeentry.TimeEntryUserSuggestionUrlStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class UserSearchConfiguration implements WebMvcConfigurer {

    private final UserSearchInterceptor userSearchInterceptor;

    UserSearchConfiguration(UserSearchInterceptor userSearchInterceptor) {
        this.userSearchInterceptor = userSearchInterceptor;
    }

    @Bean
    @ConditionalOnMissingBean(value = UserSuggestionUrlStrategy.class, name = "defaultPersonSuggestionUrlStrategy")
    UserSuggestionUrlStrategy defaultUserSuggestionUrlStrategy(TimeEntryUserSuggestionUrlStrategy timeEntryUserSuggestionUrlStrategy) {
        return timeEntryUserSuggestionUrlStrategy;
    }

    @Bean
    UserSearchUiFragmentSupplier defaultUserSearchUiFragmentSupplier() {
        return new DefaultUserSearchUiFragmentSupplier();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userSearchInterceptor);
    }
}
