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

    // note that Europe/Berlin zoneId has to be considered currently -> UTC +1 or +2
    // assumption: application is running with UTC
    // -> therefore using 3 AM as scheduled cron
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "checkOvertimePublish")
    void scheduledCheckDayLockedPublish() {
        dayLockedSchedulerService.checkDayLockedAndPublish();
    }
}
