package de.focusshift.zeiterfassung.tenancy.authentication;

import de.focusshift.zeiterfassung.tenancy.configuration.multi.ConditionalOnMultiTenantMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMultiTenantMode
@EnableConfigurationProperties({
    ClaimTenantIdResolverProperties.class,
    LegacyTenantIdResolverProperties.class
})
class MultiTenantTenantIdResolverConfiguration {

    @Bean
    @ConditionalOnProperty(name = "zeiterfassung.tenant.resolvers.claim.enabled", havingValue = "true", matchIfMissing = true)
    ClaimTenantIdResolver claimTenantIdResolver(ClaimTenantIdResolverProperties properties) {
        return new ClaimTenantIdResolver(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "zeiterfassung.tenant.resolvers.legacy.enabled", havingValue = "true", matchIfMissing = true)
    LegacyTenantIdResolver legacyTenantIdResolver(LegacyTenantIdResolverProperties properties) {
        return new LegacyTenantIdResolver(properties);
    }
}
