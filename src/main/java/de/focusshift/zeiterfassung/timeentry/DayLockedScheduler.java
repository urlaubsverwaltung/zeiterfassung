package de.focusshift.zeiterfassung.timeentry;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class DayLockedScheduler {

    private final DayLockedSchedulerService dayLockedSchedulerService;

    DayLockedScheduler(DayLockedSchedulerService dayLockedSchedulerService) {
        this.dayLockedSchedulerService = dayLockedSchedulerService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "checkOvertimePublish")
    void scheduledCheckOvertimePublish() {
        dayLockedSchedulerService.checkLockedAndPublishOvertime();
    }
}
