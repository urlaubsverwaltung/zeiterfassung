package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that a component is only eligible for registration when multi tenant mode is enabled.
 *
 * @see Conditional
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Documented
@Conditional(IsMultiTenantMode.class)
public @interface ConditionalOnMultiTenantMode {
}
