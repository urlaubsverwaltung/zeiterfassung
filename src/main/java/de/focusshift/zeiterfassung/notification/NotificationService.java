package de.focusshift.zeiterfassung.notification;

interface NotificationService {

    void sendLockWarnings();

    void sendWeeklySummary();
}
