package de.focusshift.zeiterfassung.feedback;

import de.focusshift.zeiterfassung.email.EMailConfiguration;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static de.focusshift.zeiterfassung.email.EMailConstants.DEFAULT_LOCALE;
import static org.assertj.core.api.Assertions.assertThat;

class UserFeedbackMailTemplateTest {

    private final TemplateEngine sut = new EMailConfiguration().emailTemplateEngine();

    @Test
    void ensureSenderIsNotMarkedWhenEmailIsNotUnverified() {

        final String mail = sut.process("text/user-feedback.txt", context(false));

        assertThat(mail).contains("Von:\nuser@example.org\n");
    }

    @Test
    void ensureSenderIsMarkedWhenEmailIsUnverified() {

        final String mail = sut.process("text/user-feedback.txt", context(true));

        assertThat(mail).contains("Von:\nuser@example.org (E-Mail-Adresse ist beim Login-Anbieter nicht bestätigt)\n");
    }

    private static Context context(boolean senderEMailUnverified) {
        final Context context = new Context(DEFAULT_LOCALE);
        context.setVariable("sender", "user@example.org");
        context.setVariable("senderEMailUnverified", senderEMailUnverified);
        context.setVariable("message", "awesome feedback message");
        context.setVariable("applicationName", "Zeiterfassung");
        return context;
    }
}
