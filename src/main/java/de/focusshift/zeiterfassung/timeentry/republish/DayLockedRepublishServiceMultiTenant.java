package de.focusshift.zeiterfassung.timeentry.republish;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextRunner;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.timeentry.events.DayLockedEvent;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
class DayLockedRepublishServiceMultiTenant implements DayLockedRepublishService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    // using Europe/Berlin because this is also the hard coded value in UserSettingProvider currently
    // and our application is used in germany. (same as DayLockedSchedulerServiceMultiTenant)
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Berlin");

    private final TenantContextRunner tenantContextRunner;
    private final TenantContextHolder tenantContextHolder;
    private final ApplicationEventPublisher applicationEventPublisher;

    DayLockedRepublishServiceMultiTenant(
        TenantContextRunner tenantContextRunner,
        TenantContextHolder tenantContextHolder,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.tenantContextRunner = tenantContextRunner;
        this.tenantContextHolder = tenantContextHolder;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void republishDayLockedEvents(LocalDate from, LocalDate to) {

        if (invalidRange(from, to)) {
            return;
        }

        LOG.info("Republishing DayLockedEvents for all active tenants from={} to={}", from, to);
        tenantContextRunner.runForEachActiveTenant(() -> republishForCurrentTenant(from, to)).run();
    }

    @Override
    public void republishDayLockedEvents(String tenantId, LocalDate from, LocalDate to) {

        if (invalidRange(from, to)) {
            return;
        }

        final TenantId tenant = new TenantId(tenantId);
        if (!tenant.valid()) {
            LOG.info("Ignoring republish request - 'tenantId' must be valid. tenantId={}", tenantId);
            return;
        }

        LOG.info("Republishing DayLockedEvents for tenantId={} from={} to={}", tenantId, from, to);
        try {
            tenantContextHolder.runInTenantIdContext(tenant, () -> republishForCurrentTenant(from, to));
        } catch (Exception exception) {
            LOG.error("Unexpected error while republishing DayLockedEvents for tenantId={}.", tenantId, exception);
        }
    }

    private boolean invalidRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            LOG.info("Ignoring republish request - 'from' and 'to' must both be present. from={} to={}", from, to);
            return true;
        }
        if (from.isAfter(to)) {
            LOG.info("Ignoring republish request - 'from' must not be after 'to'. from={} to={}", from, to);
            return true;
        }
        return false;
    }

    private void republishForCurrentTenant(LocalDate from, LocalDate to) {
        // 'to' is inclusive, therefore iterate until the day after
        from.datesUntil(to.plusDays(1)).forEach(day -> {
            DayLockedEvent event = of(day);
            LOG.debug("Republishing DayLockedEvent for date={} zoneId={}", event.date(), event.zoneId());
            applicationEventPublisher.publishEvent(event);
        });
    }

    private static @NonNull DayLockedEvent of(LocalDate day) {
        return new DayLockedEvent(day, ZONE_ID);
    }
}
