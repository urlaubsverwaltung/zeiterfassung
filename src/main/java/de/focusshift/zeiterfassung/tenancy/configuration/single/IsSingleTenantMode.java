package de.focusshift.zeiterfassung.tenancy.configuration.single;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.SINGLE;

public class IsSingleTenantMode implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        final String mode = context.getEnvironment()
            .getProperty("zeiterfassung.tenant.mode", SINGLE);
        return SINGLE.equalsIgnoreCase(mode);
    }
}
