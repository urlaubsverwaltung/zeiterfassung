package de.focusshift.zeiterfassung.email;

import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.StringUtils.hasText;

@Service
public class EMailService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final boolean IS_MULTIPART = true;
    private final JavaMailSender mailSender;
    private final String from;
    private final String fromDisplayName;
    private final String replyTo;
    private final String replayToDisplayName;

    @Autowired
    public EMailService(JavaMailSender javaMailSender, EMailConfigurationProperties properties) {
        this.mailSender = javaMailSender;
        this.from = properties.getFrom();
        this.fromDisplayName = properties.getFromDisplayName();
        this.replyTo = properties.getReplyTo();
        this.replayToDisplayName = properties.getReplyToDisplayName();
    }

    /**
     * Sends a mail to the given user, but only if the identity provider has verified the email address of the user.
     * Users with an unverified or unknown verification state do not receive the mail.
     *
     * @param recipient user receiving the mail
     */
    public void sendMail(TenantUser recipient, String subject, String plainText, String htmlText) throws MessagingException {
        if (!recipient.isEMailVerified()) {
            LOG.debug("Not sending mail to user with id={}, because the email address is not verified.", recipient.id());
            return;
        }

        send(recipient.eMail().value(), subject, plainText, htmlText);
    }

    /**
     * Sends a mail to an address configured by the operator of the application, e.g. the recipient of user feedback.
     * The address does not belong to a user, therefore it is not checked for a verification.
     *
     * @param to configured email address receiving the mail
     */
    public void sendMailToConfiguredAddress(String to, String subject, String plainText, String htmlText) throws MessagingException {
        send(to, subject, plainText, htmlText);
    }

    private void send(String to, String subject, String plainText, String htmlText) throws MessagingException {
        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper message = new MimeMessageHelper(mimeMessage, IS_MULTIPART);
        message.setFrom(generateMailAddressAndDisplayName(from, fromDisplayName));
        message.setTo(to);
        message.setSubject(subject);
        message.setReplyTo(generateMailAddressAndDisplayName(replyTo, replayToDisplayName));
        if (hasText(htmlText)) {
            message.setText(plainText, htmlText);
        } else {
            message.setText(plainText);
        }
        this.mailSender.send(mimeMessage);
    }

    private String generateMailAddressAndDisplayName(String address, String displayName) {
        return String.format("%s <%s>", displayName, address);
    }
}
