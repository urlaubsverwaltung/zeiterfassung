package de.focusshift.zeiterfassung.footer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class FooterConfiguration implements WebMvcConfigurer {

    @Bean
    FooterControllerAdvice footerControllerAdvice(@Value("${info.app.version}") String applicationVersion) {
        return new FooterControllerAdvice(applicationVersion);
    }

    @Bean
    WebMvcConfigurer infoBannerWebMvcConfigurer(FooterControllerAdvice footerControllerAdvice) {
        return new InfoBannerWebMvcConfigurer(footerControllerAdvice);
    }

    static class InfoBannerWebMvcConfigurer implements WebMvcConfigurer {

        private final FooterControllerAdvice footerControllerAdvice;

        InfoBannerWebMvcConfigurer(FooterControllerAdvice footerControllerAdvice) {
            this.footerControllerAdvice = footerControllerAdvice;
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(footerControllerAdvice);
        }
    }
}
