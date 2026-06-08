package de.focusshift.zeiterfassung.gitactivity;

public record AnchorEvent(
    String icon,
    String summary,
    String time,
    String prefilledComment,
    String eventId,
    boolean logged,
    String commitUrl   // nullable; only set for standalone commits
) {}
