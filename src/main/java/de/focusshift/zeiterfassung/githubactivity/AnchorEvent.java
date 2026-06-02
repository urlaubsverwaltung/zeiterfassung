package de.focusshift.zeiterfassung.githubactivity;

public record AnchorEvent(
    String icon,
    String summary,
    String time,
    String prefilledComment,
    String eventId,
    boolean logged
) {}
