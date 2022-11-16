package de.focusshift.zeiterfassung.feedback;

import de.focusshift.zeiterfassung.tenantuser.EMailAddress;

public record FeedbackGivenEvent(EMailAddress sender, String message) {
}
