package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.timeentry.DayLockedEvent;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccount;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountService;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
class OvertimePublisher {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final OvertimeService overtimeService;
    private final OvertimeAccountService overtimeAccountService;
    private final ApplicationEventPublisher applicationEventPublisher;

    OvertimePublisher(
        OvertimeService overtimeService,
        OvertimeAccountService overtimeAccountService,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.overtimeService = overtimeService;
        this.overtimeAccountService = overtimeAccountService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener
    public void publishOvertime(DayLockedEvent event) {
        LOG.info("TimeEntry Locking enabled -> fetch timeEntries and publish overtime entries.");

        final Map<UserIdComposite, OvertimeAccount> overtimeAccountByUserId = overtimeAccountService.getAllOvertimeAccounts();

        overtimeService.getOvertimeForDate(event.date()).forEach((userIdComposite, overtimeHours) -> {

            // currently we just know whether overtime is allowed or not.
            // this will change in the future to know from $date to $date and maybe more granular stuff.
            // for the moment: publish hasMadeOvertime? yes or no.
            final OvertimeAccount overtimeAccount = overtimeAccountByUserId.get(userIdComposite);
            if (overtimeAccount.isAllowed()) {
                final UserHasMadeOvertimeEvent overtimeEvent = new UserHasMadeOvertimeEvent(userIdComposite, event.date(), overtimeHours);
                LOG.debug("publish {}", overtimeEvent);
                applicationEventPublisher.publishEvent(overtimeEvent);
            }
        });
    }
}
