package de.focusshift.zeiterfassung.email;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.UserStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EMailServiceTest {

    private EMailService sut;

    @Mock
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        final EMailConfigurationProperties properties = new EMailConfigurationProperties();
        properties.setFrom("from@example.org");
        properties.setFromDisplayName("Zeiterfassung");
        properties.setReplyTo("reply-to@example.org");
        properties.setReplyToDisplayName("Zeiterfassung");

        sut = new EMailService(javaMailSender, properties);
    }

    @Test
    void ensureMailIsSentToUserWithVerifiedEmail() throws MessagingException {

        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        sut.sendMail(user(true), "subject", "plain text", "");

        final ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getAllRecipients()).containsExactly(new InternetAddress("batman@example.org"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    void ensureNoMailIsSentToUserWithUnverifiedOrUnknownEmail(Boolean eMailVerified) throws MessagingException {

        sut.sendMail(user(eMailVerified), "subject", "plain text", "");

        verify(javaMailSender, never()).createMimeMessage();
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void ensureMailIsSentToConfiguredAddress() throws MessagingException {

        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        sut.sendMailToConfiguredAddress("feedback@example.org", "subject", "plain text", "");

        final ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getAllRecipients()).containsExactly(new InternetAddress("feedback@example.org"));
    }

    private static TenantUser user(Boolean eMailVerified) {
        final Instant now = Instant.now();
        return new TenantUser("uuid", 1L, "Bruce", "Wayne", new EMailAddress("batman@example.org"), eMailVerified, now, Set.of(), now, now, null, null, UserStatus.ACTIVE);
    }
}
