package de.focusshift.zeiterfassung.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class FrameDataProviderConfiguration {

    @Bean
    FrameDataProvider headerScriptControllerAdvice(MenuProperties menuProperties) {
        return new FrameDataProvider(menuProperties);
    }

    @Bean
    WebMvcConfigurer headerScriptWebMvcConfigurer(FrameDataProvider frameDataProvider) {
        return new FrameDataProviderWebMvcConfigurer(frameDataProvider);
    }

    static class FrameDataProviderWebMvcConfigurer implements WebMvcConfigurer {

        private final FrameDataProvider frameDataProvider;

        FrameDataProviderWebMvcConfigurer(FrameDataProvider frameDataProvider) {
            this.frameDataProvider = frameDataProvider;
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(frameDataProvider);
        }
    }
}
