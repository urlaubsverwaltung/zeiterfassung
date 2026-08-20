package de.focusshift.zeiterfassung.branding;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(BrandingConfigProperties.class)
class BrandingConfiguration {

    @Bean
    BrandingDataProvider brandingDataProvider(BrandingConfigProperties brandingConfigProperties) {
        return new BrandingDataProvider(brandingConfigProperties);
    }

    @Bean
    WebMvcConfigurer brandingDataProviderWebMvcConfigurer(BrandingDataProvider brandingDataProvider) {
        return new BrandingWebMvcConfigurer(brandingDataProvider);
    }

    static class BrandingWebMvcConfigurer implements WebMvcConfigurer {

        private final BrandingDataProvider brandingDataProvider;

        BrandingWebMvcConfigurer(BrandingDataProvider brandingDataProvider) {
            this.brandingDataProvider = brandingDataProvider;
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(brandingDataProvider);
        }
    }
}
