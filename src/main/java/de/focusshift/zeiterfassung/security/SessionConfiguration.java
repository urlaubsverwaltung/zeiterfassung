package de.focusshift.zeiterfassung.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

@Configuration
@EnableSpringHttpSession
class SessionConfiguration {

    @Bean
    FindByIndexNameSessionRepository<Session> sessionRepository() {
        return new IndexNameMapSessionRepository();
    }
}
