package de.focusshift.zeiterfassung.config;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.time.Clock;
import java.util.Date;
import java.util.Set;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Guards that time is always derived from the application {@link Clock} bean
 * (see {@link ClockConfig}) so it stays controllable in tests and consistent
 * with the configured time zone.
 */
@AnalyzeClasses(packages = "de.focusshift.zeiterfassung", importOptions = ImportOption.DoNotIncludeTests.class)
class ClockUsageArchTest {

    private static final Set<String> SYSTEM_CLOCK_METHODS = Set.of("systemUTC", "systemDefaultZone", "system");
    private static final Set<String> SYSTEM_TIME_METHODS = Set.of("currentTimeMillis", "nanoTime");

    @ArchTest
    static final ArchRule noNowWithoutClock =
        noClasses()
            .should().callMethodWhere(nowWithoutClockArgument())
            .as("no class may call a java.time now()-method without passing the application Clock")
            .because("time must be controllable in tests and consistent with the configured Clock, "
                + "e.g. use LocalDate.now(clock) / ZonedDateTime.now(clock.withZone(zoneId)) instead of now() or now(zoneId)")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule noSystemClockOutsideClockConfig =
        noClasses()
            .that().doNotBelongToAnyOf(ClockConfig.class)
            .should().callMethodWhere(systemClockOrSystemTime())
            .as("no class other than ClockConfig may obtain a system clock / system time directly")
            .because("the Clock bean is the single source of truth for time; inject it instead of Clock.systemUTC() / System.currentTimeMillis()")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule noNoArgDate =
        noClasses()
            .should().callConstructorWhere(
                describe("new java.util.Date() (no-arg, reads system time)", call ->
                    call.getTargetOwner().isEquivalentTo(Date.class)
                        && call.getTarget().getRawParameterTypes().isEmpty()))
            .as("no class may use the no-arg java.util.Date() constructor")
            .because("it reads system time; derive time from the Clock bean")
            .allowEmptyShould(true);

    private static DescribedPredicate<JavaCall<?>> nowWithoutClockArgument() {
        return describe("call to a java.time now()-method without a Clock argument", call -> {
            if (!"now".equals(call.getTarget().getName())) {
                return false;
            }
            if (!call.getTargetOwner().getPackageName().startsWith("java.time")) {
                return false;
            }
            return call.getTarget().getRawParameterTypes().stream()
                .noneMatch(parameter -> parameter.isEquivalentTo(Clock.class));
        });
    }

    private static DescribedPredicate<JavaCall<?>> systemClockOrSystemTime() {
        return describe("call to Clock.system*/System.currentTimeMillis()/System.nanoTime()", call -> {
            final JavaClass owner = call.getTargetOwner();
            final String name = call.getTarget().getName();
            if (owner.isEquivalentTo(Clock.class)) {
                return SYSTEM_CLOCK_METHODS.contains(name);
            }
            if (owner.isEquivalentTo(System.class)) {
                return SYSTEM_TIME_METHODS.contains(name);
            }
            return false;
        });
    }
}
