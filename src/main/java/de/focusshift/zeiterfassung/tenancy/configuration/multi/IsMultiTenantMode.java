package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;
import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.SINGLE;

public class IsMultiTenantMode implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        final String mode = context.getEnvironment()
            .getProperty("zeiterfassung.tenant.mode", SINGLE);
        return MULTI.equalsIgnoreCase(mode);
    }
}
