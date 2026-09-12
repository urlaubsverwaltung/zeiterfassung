package de.focusshift.zeiterfassung.branding;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BrandingConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(BrandingConfiguration.class);

    @Test
    void ensureBrandingDataProviderExists() {
        contextRunner
            .withPropertyValues("zeiterfassung.branding.name=Custom Name")
            .run(context -> assertThat(context).hasSingleBean(BrandingDataProvider.class));
    }

    @Test
    void ensureContextFailsWhenNameIsEmpty() {
        contextRunner
            .withPropertyValues("zeiterfassung.branding.name=")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void ensureContextFailsWhenNameIsMissing() {
        contextRunner
            .run(context -> assertThat(context).hasFailed());
    }
}
