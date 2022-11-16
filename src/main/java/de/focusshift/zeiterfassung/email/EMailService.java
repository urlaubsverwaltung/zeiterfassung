package de.focusshift.zeiterfassung.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
public class EMailService {

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

    public void sendMail(String to, String subject, String plainText, String htmlText) throws MessagingException {
        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper message = new MimeMessageHelper(mimeMessage, IS_MULTIPART);
        message.setFrom(generateMailAddressAndDisplayName(from, fromDisplayName));
        message.setTo(to);
        message.setSubject(subject);
        message.setReplyTo(generateMailAddressAndDisplayName(replyTo, replayToDisplayName));
        message.setText(plainText, htmlText);
        this.mailSender.send(mimeMessage);
    }

    private String generateMailAddressAndDisplayName(String address, String displayName) {
        return String.format("%s <%s>", displayName, address);
    }
}
