package de.focusshift.zeiterfassung.feedback.events;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;

/**
 * @param senderEMailVerified whether the identity provider has verified the email address of the sender, {@code null} if unknown
 */
public record FeedbackGivenEvent(EMailAddress sender, Boolean senderEMailVerified, String message) {

    /**
     * @return {@code true} if the identity provider explicitly states that the email address of the sender is not
     * verified, {@code false} if it is verified or unknown.
     */
    public boolean isSenderEMailUnverified() {
        return Boolean.FALSE.equals(senderEMailVerified);
    }
}
