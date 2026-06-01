package de.focusshift.zeiterfassung.notification;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class NotificationScheduler {

    private final NotificationService notificationService;

    NotificationScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 06:00 UTC = 07:00–08:00 Berlin (before users start work)
    @Scheduled(cron = "0 0 6 * * *")
    @SchedulerLock(name = "sendLockWarningNotifications")
    void lockWarnings() {
        notificationService.sendLockWarnings();
    }

    // 11:00 UTC Monday = 12:00–13:00 Berlin noon
    @Scheduled(cron = "0 0 11 * * MON")
    @SchedulerLock(name = "sendWeeklySummaryNotifications")
    void weeklySummary() {
        notificationService.sendWeeklySummary();
    }
}
