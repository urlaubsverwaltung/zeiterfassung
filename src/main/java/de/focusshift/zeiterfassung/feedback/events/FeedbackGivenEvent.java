package de.focusshift.zeiterfassung.feedback.events;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;

public record FeedbackGivenEvent(EMailAddress sender, String message) {
}
