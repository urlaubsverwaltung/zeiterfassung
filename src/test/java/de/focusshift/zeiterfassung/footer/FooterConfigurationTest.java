package de.focusshift.zeiterfassung.footer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FooterConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(FooterConfiguration.class);

    @Test
    void ensureFooterControllerAdviceExists() {
        contextRunner
            .run(context -> assertThat(context).hasSingleBean(FooterControllerAdvice.class));
    }
}
